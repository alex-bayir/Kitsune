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
import me.zhanghai.android.fastscroll.FastScrollerBuilder;
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
    final RecyclerView rv;
    final CustomAdapter<Chapter> adapter;
    final TextView noItems;
    final Manga manga;
    public static final int RA=R.string.remove_all_chapters,RS=R.string.remove_selected_chapters,SA=R.string.save_all_chapters,SS=R.string.save_selected_chapters;
    public ChaptersPage(ViewGroup parent,Manga manga){
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_recyclerview_list,parent,false));
        this.manga=manga;
        noItems=itemView.findViewById(R.id.text);
        rv=itemView.findViewById(R.id.rv_list);
        adapter=new CustomAdapter<>(itemView.getContext(),manga,manga.getChapters(),R.layout.item_chapter,this,null,rv,"ChapterSelector");
        rv.getLayoutParams().height=ViewGroup.LayoutParams.WRAP_CONTENT;
        rv.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
        setReversed(PreferenceManager.getDefaultSharedPreferences(itemView.getContext()).getBoolean(Constants.reversed,false));
        rv.addItemDecoration(new DividerItemDecoration(rv.getContext(), DividerItemDecoration.VERTICAL));
        rv.setVerticalScrollBarEnabled(false);
        (new FastScrollerBuilder(rv)).build();
        noItems.setText(R.string.No_chapters);
        noItems.setVisibility(View.GONE);
        if(manga.getHistory()!=null){rv.scrollToPosition(adapter.getPosition(manga.getHistory().getChapter())-5);}
        Utils.registerOnEmptyAdapterRunnable(adapter,()->noItems.setVisibility(adapter.getItemCount()==0 ? View.VISIBLE : View.GONE));
        adapter.getTracker().addObserver(new SelectionTracker.SelectionObserver<>() {
            @Override
            public void onSelectionChanged() {
                new Handler(Looper.getMainLooper()).post(()->((PreviewActivity)parent.getContext()).setSelectMode(adapter.getTracker().hasSelection()));
            }
        });
    }

    @Override
    public void bind(Object obj) {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(View v, int index){
        if(adapter.getTracker().hasSelection()){
            select_deselect(adapter.getTracker(),Integer.toUnsignedLong(index));
        }else{
            itemView.getContext().startActivity(new Intent(itemView.getContext(),ReaderActivity.class).putExtra(Constants.hash,manga.hashCode()).putExtra(Constants.chapter, index));
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
    public boolean setReversed(boolean reversed){((LinearLayoutManager)rv.getLayoutManager()).setReverseLayout(reversed); return reversed;}

    public void search(String query){
        int index=(query!=null && query.length()>0) ? adapter.search(query) : -1;
        if(index>=0){rv.scrollToPosition(index);}
    }

    public void action(int action){action(itemView.getContext(),manga,action,adapter);}
    public static void action(Context context, Manga manga, int action, CustomAdapter<Chapter> adapter){
        new Thread(()->{
            Handler handler=new Handler(Looper.getMainLooper());
            boolean deleted=false;
            switch (action){
                case RA:
                    for(int i=0;i<manga.getChapters().size();i++){
                        manga.clearChapter(i); int f=i; handler.post(()->adapter.notifyItemChanged(f));
                    }
                    manga.deleteAllPages(); handler.postDelayed(adapter::notifyDataSetChanged,100);
                    manga.save(); deleted=true; adapter.getTracker().clearSelection(); break;
                case RS:
                    for(Long i:adapter.getTracker().getSelection()){
                        manga.clearChapter(i.intValue()); handler.post(()->adapter.notifyItemChanged(i.intValue()));
                    }
                    if(adapter.getTracker().getSelection().size()==manga.getChapters().size()){
                        manga.deleteAllPages(); handler.postDelayed(adapter::notifyDataSetChanged,100);
                    }
                    manga.save(); deleted=true; adapter.getTracker().clearSelection(); break;
                case SA:
                    manga.loadChapters(context,new LoadService.Task(manga,0,manga.getChapters().size()-1));
                    adapter.getTracker().clearSelection(); break;
                case SS:
                    manga.loadChapters(context,new LoadService.Task(manga,adapter.getTracker().getSelection()));
                    adapter.getTracker().clearSelection(); break;
                default: break;
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
