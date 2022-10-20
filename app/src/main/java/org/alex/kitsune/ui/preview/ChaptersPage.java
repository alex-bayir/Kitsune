package org.alex.kitsune.ui.preview;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
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
import java.util.ArrayList;
import java.util.Objects;

public class ChaptersPage extends PreviewHolder implements HolderListener {
    final RecyclerView rv;
    final CustomAdapter<Chapter> adapter;
    final TextView noItems;
    final Manga manga;
    public static final int RC=R.string.RC,RL=R.string.RL,RA=R.string.RA,SC=R.string.SC,SL=R.string.SL,S5=R.string.S5,S10=R.string.S10,S30=R.string.S30,SN=R.string.SN,SA=R.string.SA;
    private int[] actions;
    private int index=-1;
    public ChaptersPage(ViewGroup parent,Manga manga){
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_recyclerview_list,parent,false));
        this.manga=manga;
        noItems=itemView.findViewById(R.id.text);
        adapter=new CustomAdapter<>(itemView.getContext(),manga,manga.getChapters(),R.layout.item_chapter,this,null);
        rv=itemView.findViewById(R.id.rv_list);
        rv.getLayoutParams().height=ViewGroup.LayoutParams.WRAP_CONTENT;
        rv.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
        setReversed(PreferenceManager.getDefaultSharedPreferences(itemView.getContext()).getBoolean(Constants.reversed,false));
        rv.addItemDecoration(new DividerItemDecoration(rv.getContext(), DividerItemDecoration.VERTICAL));
        rv.setVerticalScrollBarEnabled(false);
        (new FastScrollerBuilder(rv)).build();
        noItems.setText(R.string.No_chapters);
        noItems.setVisibility(View.GONE);
        rv.setAdapter(adapter);
        if(manga.getHistory()!=null){rv.scrollToPosition(adapter.getPosition(manga.getHistory().getChapter())-5);}
        Utils.registerOnEmptyAdapterRunnable(adapter,()->noItems.setVisibility(adapter.getItemCount()==0 ? View.VISIBLE : View.GONE));
    }

    @Override
    public void bind(Object obj) {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(View v, int index){
        itemView.getContext().startActivity(new Intent(itemView.getContext(),ReaderActivity.class).putExtra(Constants.hash,manga.hashCode()).putExtra(Constants.chapter, index));
    }

    @Override
    public boolean onItemLongClick(View v, int index){
        Chapter chapter=manga.getChapters().get(index); int size=manga.getChapters().size();
        ArrayList<Integer> actions=new ArrayList<>();
        if(manga.checkChapter(chapter)){
            actions.add(RC);
            if(index>0)
            actions.add(RL);
            actions.add(RA);
        }else{
            actions.add(SC);
            actions.add(SL);
            if(size-index>5)
            actions.add(S5);
            if(size-index>10)
            actions.add(S10);
            if(size-index>30)
            actions.add(S30);
            if(size-index>1)
            actions.add(SN);
            actions.add(SA);
        }
        this.actions=new int[actions.size()];
        String[] actionsStrings=new String[this.actions.length];
        for(int i=0;i<this.actions.length;i++){
            this.actions[i]=actions.get(i);
            actionsStrings[i]=v.getResources().getString(this.actions[i]);
        }
        this.index=index;
        new AlertDialog.Builder(Objects.requireNonNull(itemView.getContext())).setTitle(chapter.text(itemView.getContext())).setItems(actionsStrings, actionsListener).create().show();
        return false;
    }
    public boolean setReversed(boolean reverced){((LinearLayoutManager)rv.getLayoutManager()).setReverseLayout(reverced); return reverced;}
    public final DialogInterface.OnClickListener actionsListener=new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which){action(actions[which]);}
    };

    public void search(String query){
        int index=(query!=null && query.length()>0) ? adapter.search(query) : -1;
        if(index>=0){rv.scrollToPosition(index);}
    }

    public void action(int action){action(itemView.getContext(),manga,action,adapter,index);}
    public static void action(Context context, Manga manga, int action, CustomAdapter adapter,int index){
        boolean deleted=false;
        switch (action){
            case RC: manga.clearChapters(index,index+1); new Handler(Looper.getMainLooper()).post(adapter::notifyDataSetChanged); manga.save(); deleted=true; break;
            case RL: manga.clearChapters(0,index+1); new Handler(Looper.getMainLooper()).post(adapter::notifyDataSetChanged); manga.save(); deleted=true; break;
            case RA: manga.deleteAllPages(); new Handler(Looper.getMainLooper()).post(adapter::notifyDataSetChanged); manga.save(); deleted=true; break;
            case SC: manga.loadChapters(context,new LoadService.Task(manga,index,index+1)); break;
            case SL: manga.loadChapters(context,new LoadService.Task(manga,0,index+1)); break;
            case S5: manga.loadChapters(context,new LoadService.Task(manga,index,index+6)); break;
            case S10: manga.loadChapters(context,new LoadService.Task(manga,index,index+11)); break;
            case S30: manga.loadChapters(context,new LoadService.Task(manga,index,index+31)); break;
            case SN: manga.loadChapters(context,new LoadService.Task(manga,index,manga.getChapters().size())); break;
            case SA: manga.loadChapters(context,new LoadService.Task(manga,0,manga.getChapters().size())); break;
            default: break;
        }
        MangaService.allocate(manga,false);
        if(deleted){
            context.sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash,manga.hashCode()).putExtra(Constants.option,Constants.delete));
        }
    }
}
