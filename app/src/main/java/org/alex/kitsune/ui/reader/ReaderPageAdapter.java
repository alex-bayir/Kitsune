package org.alex.kitsune.ui.reader;

import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.*;
import org.alex.kitsune.manga.Chapter;
import org.alex.kitsune.manga.Manga;

public class ReaderPageAdapter extends RecyclerView.Adapter<ReaderPageHolder> {
    Manga manga;
    Chapter chapter=null;
    private ReaderPageHolder.ScaleType scaleType=ReaderPageHolder.ScaleType.FIT_X;
    final View.OnClickListener visibleUIListener,leftClick,rightClick;
    private final  LinearLayoutManager layoutManager;
    private final SnapHelper snapHelper=new PagerSnapHelper();
    private final RecyclerView rv;


    public ReaderPageAdapter(Manga manga, RecyclerView rv, View.OnClickListener visibleUIListener,View.OnClickListener leftClick,View.OnClickListener rightClick) {
        this.manga=manga;
        this.visibleUIListener=visibleUIListener;
        this.leftClick=leftClick;
        this.rightClick=rightClick;
        this.layoutManager=new LinearLayoutManager(rv.getContext());
        this.rv=rv;
        this.rv.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
        this.rv.setLayoutManager(layoutManager);
        this.rv.setAdapter(this);
    }
    public LinearLayoutManager getLayoutManager(){return layoutManager;}
    public void setChapter(int chapter){this.chapter=manga.getChapters().get(chapter); notifyDataSetChanged();}
    public ReaderPageHolder.ScaleType getScaleType(){return scaleType;}
    public boolean isReverse(){return rv.getLayoutDirection()==View.LAYOUT_DIRECTION_RTL;}
    public int getOrientation(){return layoutManager.getOrientation();}
    public void setOrientation(int orientation,boolean reverse){
        layoutManager.setOrientation(orientation); rv.setLayoutDirection(reverse ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR); snapHelper.attachToRecyclerView(orientation==RecyclerView.HORIZONTAL ? rv : null);
    }
    public void setScaleType(ReaderPageHolder.ScaleType scaleType){
        this.scaleType=scaleType!=null ? scaleType : ReaderPageHolder.ScaleType.FIT_X;
    }
    public int getReaderMode(){return getOrientation()==RecyclerView.HORIZONTAL ? (isReverse() ? 1 : 0) : 2;}
    public int getScaleMode(){return scaleType.ordinal();}
    public void setModes(int modeR, int modeS){
        switch (modeR){
            default:
            case 0: setOrientation(RecyclerView.HORIZONTAL,false); break;
            case 1: setOrientation(RecyclerView.HORIZONTAL,true); break;
            case 2: setOrientation(RecyclerView.VERTICAL,false); break;
        }
        setScaleType(ReaderPageHolder.ScaleType.valueOf(modeS));
        notifyItemRangeChanged(0,getItemCount());
    }

    @Override public int getItemViewType(int position){return getOrientation();}

    @Override
    public ReaderPageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ReaderPageHolder(parent,viewType==RecyclerView.VERTICAL,manga,visibleUIListener,leftClick,rightClick);
    }

    @Override
    public void onBindViewHolder(ReaderPageHolder holder, int position) {
        holder.onBind(position,chapter,scaleType);
    }

    @Override
    public int getItemCount(){return ((chapter!=null && chapter.getPages()!=null) ? chapter.getPages().size() : 0);}

    public Chapter getChapter(){return chapter;}
}
