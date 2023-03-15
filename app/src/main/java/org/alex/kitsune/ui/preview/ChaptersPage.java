package org.alex.kitsune.ui.preview;


import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.*;
import org.alex.kitsune.commons.FastScroller;
import org.alex.kitsune.commons.HolderListener;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.R;
import org.alex.kitsune.services.LoadService;
import org.alex.kitsune.manga.Chapter;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.services.MangaService;
import org.alex.kitsune.ui.reader.ReaderActivity;
import org.alex.kitsune.utils.Utils;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class ChaptersPage extends PreviewHolder implements HolderListener {
    RecyclerView rv;
    CustomAdapter<Chapter> adapter;
    TextView noItems;
    public static final int RA=R.string.remove_all_chapters,RS=R.string.remove_selected_chapters,SA=R.string.save_all_chapters,SS=R.string.save_selected_chapters;
    public ChaptersPage(ViewGroup parent){
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_recyclerview_list,parent,false));
        noItems=itemView.findViewById(R.id.text);
        rv=itemView.findViewById(R.id.rv_list);
        adapter=new CustomAdapter<>(R.layout.item_chapter,this,null,rv,"ChapterSelector");
        rv.getLayoutParams().height=ViewGroup.LayoutParams.WRAP_CONTENT;
        rv.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
        setReversed(PreferenceManager.getDefaultSharedPreferences(itemView.getContext()).getBoolean(Constants.reversed,false));
        rv.addItemDecoration(new DividerItemDecoration(rv.getContext(), DividerItemDecoration.VERTICAL));
        FastScroller.createDefault(rv).setPadding(30,0,30,0).setOnStateChangeListener(state -> adapter.setSelectable(state!=FastScroller.STATE_DRAGGING));
        noItems.setText(R.string.No_chapters);
        noItems.setVisibility(View.VISIBLE);
        Utils.registerAdapterDataChangeRunnable(adapter,()-> noItems.setVisibility(adapter.getItemCount()==0 ? View.VISIBLE : View.GONE));
        adapter.getTracker().addObserver(new SelectionTracker.SelectionObserver<>() {
            @Override
            public void onSelectionChanged() {
                new Handler(Looper.getMainLooper()).post(()->((PreviewActivity)parent.getContext()).setSelectMode(adapter.getTracker().hasSelection()));
            }
        });
    }

    @Override
    public void bind(Manga manga) {
        adapter.setManga(manga);
    }

    @Override
    public void onItemClick(View v, int index){
        if(adapter.getTracker().hasSelection()){
            select_deselect(adapter.getTracker(),Integer.toUnsignedLong(index));
        }else{
            itemView.getContext().startActivity(new Intent(itemView.getContext(),ReaderActivity.class).putExtra(Constants.hash,adapter.manga.hashCode()).putExtra(Constants.chapter, index));
        }
    }

    @Override
    public boolean onItemLongClick(View v, int index){
        if(adapter.getTracker().getSelection().size()>1){
            adapter.getTracker().setItemsSelected(getNearestRangeSelected(adapter.getTracker().getSelection(),index), true);
        }
        return false;
    }
    private boolean select_deselect(SelectionTracker<Long> tracker,long key){
        return tracker.isSelected(key)?tracker.deselect(key):tracker.select(key);
    }
    public boolean setReversed(boolean reversed){
        if(rv.getLayoutManager() instanceof LinearLayoutManager llm){llm.setReverseLayout(reversed);} return reversed;
    }

    public void search(Context context, String query){
        int index=(query!=null && query.length()>0) ? adapter.search(context,query) : -1;
        if(index>=0){rv.scrollToPosition(index);}
    }

    public void scrollToHistory(){
        if(adapter.manga!=null && adapter.manga.getHistory()!=null){rv.scrollToPosition(Math.min(adapter.getPosition(adapter.manga.getHistory().getChapter())+5,adapter.getItemCount()-1));}
    }
    public void action(int action){action(itemView.getContext(),adapter.manga,action,adapter);}
    public static void action(Context context, Manga manga, int action, CustomAdapter<Chapter> adapter){
        if(manga==null){return;}
        new Thread(()->{
            Handler handler=new Handler(Looper.getMainLooper());
            boolean deleted=false;
            switch (action) {
                case RA -> {
                    for (int i = 0; i < manga.getChapters().size(); i++) {
                        manga.clearChapter(i); int f=i; handler.post(() -> adapter.notifyItemChanged(f));
                    }
                    manga.deleteAllPages(); handler.postDelayed(adapter::notifyDataSetChanged, 100);
                    manga.save(); deleted=true; adapter.getTracker().clearSelection();
                }
                case RS -> {
                    for (int i : Utils.convert(adapter.getTracker().getSelection())) {
                        manga.clearChapter(i); handler.post(() -> adapter.notifyItemChanged(i));
                    }
                    if (adapter.getTracker().getSelection().size() == manga.getChapters().size()) {
                        manga.deleteAllPages(); handler.postDelayed(adapter::notifyDataSetChanged, 100);
                    }
                    manga.save(); deleted = true; adapter.getTracker().clearSelection();
                }
                case SA -> {
                    manga.loadChapters(context, new LoadService.Task(manga, 0, manga.getChapters().size() - 1));
                    adapter.getTracker().clearSelection();
                }
                case SS -> {
                    manga.loadChapters(context, new LoadService.Task(manga, adapter.getTracker().getSelection()));
                    adapter.getTracker().clearSelection();
                }
            }
            MangaService.allocate(manga,false);
            if(deleted){
                context.sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash,manga.hashCode()).putExtra(Constants.option,Constants.delete));
            }
        }).start();
    }

    private Iterable<Long> getNearestRangeSelected(Selection<Long> selection, int end){
        int start=-1, min=Integer.MAX_VALUE;
        for(Long l:selection){
            int tmp=Math.abs(l.intValue()-end);
            if(tmp<min && tmp!=0){
                min=tmp; start=l.intValue();
            }
        }
        return LongStream.rangeClosed(Math.min(start,end),Math.max(start,end)).boxed().collect(Collectors.toList());
    }
}
