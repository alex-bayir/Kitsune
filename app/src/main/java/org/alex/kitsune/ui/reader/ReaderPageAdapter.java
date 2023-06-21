package org.alex.kitsune.ui.reader;

import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.*;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.book.Chapter;
import org.alex.kitsune.ui.reader.ReaderPageHolder.Modes;
import org.jetbrains.annotations.NotNull;

public class ReaderPageAdapter extends RecyclerView.Adapter<ReaderPageHolder> {
    Book book;
    Chapter chapter=null;
    private Modes modes=new Modes(ReaderPageHolder.ReaderMode.HorizontalRight, ReaderPageHolder.ScaleMode.FIT_X);
    final View.OnClickListener visibleUIListener,leftClick,rightClick;
    private final  LinearLayoutManager layoutManager;
    private final SnapHelper snapHelper=new PagerSnapHelper();
    private final RecyclerView rv;
    private boolean showTranslate=false;


    public ReaderPageAdapter(Book book, RecyclerView rv, View.OnClickListener visibleUIListener, View.OnClickListener leftClick, View.OnClickListener rightClick) {
        this.book=book;
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
    public void setChapter(int chapter){this.chapter=book.getChapters().get(chapter); notifyDataSetChanged();}
    public int getOrientation(){return layoutManager.getOrientation();}
    private void setOrientation(int orientation,boolean snap,boolean reverse){
        layoutManager.setOrientation(orientation); rv.setLayoutDirection(reverse ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR); snapHelper.attachToRecyclerView(snap ? rv : null);
    }
    public void setShowTranslate(boolean show){
        showTranslate=show;
        notifyItemRangeChanged(0,getItemCount());
    }
    public void invertShowTranslate(){
        setShowTranslate(!isShowTranslate());
    }
    public boolean isShowTranslate(){return showTranslate;}
    public int getReaderMode(){return modes.R.ordinal();}
    public int getScaleMode(){return modes.S.ordinal();}
    public void setModes(int modeR, int modeS){
        this.modes=new ReaderPageHolder.Modes(modeR,modeS);
        switch (this.modes.R) {
            case HorizontalRight -> setOrientation(RecyclerView.HORIZONTAL, true,false);
            case HorizontalLeft -> setOrientation(RecyclerView.HORIZONTAL, true,true);
            case Vertical -> setOrientation(RecyclerView.VERTICAL,true, false);
            case VerticalWeb -> setOrientation(RecyclerView.VERTICAL,false, false);
        }
        notifyAllChanged();
    }

    @Override public int getItemViewType(int position){return modes.hashCode();}

    @NotNull
    @Override
    public ReaderPageHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ReaderPageHolder(parent,ReaderPageHolder.Modes.valueOf(viewType),book,visibleUIListener,leftClick,rightClick);
    }

    @Override
    public void onBindViewHolder(ReaderPageHolder holder, int position) {
        holder.onBind(position,chapter,showTranslate);
    }

    @Override
    public int getItemCount(){return ((chapter!=null && chapter.getPages()!=null) ? chapter.getPages().size() : 0);}

    public Chapter getChapter(){return chapter;}

    public void notifyAllChanged(){
        notifyItemRangeChanged(0,getItemCount());
    }
}
