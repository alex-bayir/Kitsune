package org.alex.kitsune.book.views;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import io.github.douglasjunior.androidSimpleTooltip.OverlayView;
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip;
import org.alex.kitsune.R;
import com.alex.ratingbar.RatingBar;
import org.alex.kitsune.commons.AspectRatioImageView;
import org.alex.kitsune.commons.AspectRatioPhotoView;
import org.alex.kitsune.commons.HolderClickListener;
import org.alex.kitsune.commons.ProgressDrawable;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.utils.Utils;
import java.util.*;


public class BookHolder extends RecyclerView.ViewHolder{

    private final MaterialCardView root;
    private final RelativeLayout full;
    private final AspectRatioImageView cover;
    private final TextView titleShort,title,subtitle,genres,num_saved,num_new,size;
    private final RatingBar ratingBar;
    private final Button button;
    private final ImageView line, markNew;
    private final Drawable caution;
    private final int cardWidth;
    private static final Random r=new Random();
    private final ProgressDrawable progress_drawable=new ProgressDrawable(Color.HSVToColor(new float[]{r.nextFloat()*360,1,1}));

    public BookHolder(ViewGroup parent, HolderClickListener onItem, HolderClickListener onItemButton){
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book,parent,false));
        root=(MaterialCardView)itemView;
        cover=root.findViewById(R.id.cover);
        caution=cover.getDrawable();
        cardWidth=cover.getLayoutParams().width;
        titleShort=root.findViewById(R.id.title_short);
        full=root.findViewById(R.id.full);
        title=root.findViewById(R.id.title);
        subtitle=root.findViewById(R.id.subtitle);
        genres=root.findViewById(R.id.genres);
        ratingBar=root.findViewById(R.id.ratingBar);
        size=root.findViewById(R.id.images_size);
        button=root.findViewById(R.id.button_continue);
        num_saved=root.findViewById(R.id.num_saved);
        num_new=root.findViewById(R.id.num_new);
        line=root.findViewById(R.id.line);
        line.setImageDrawable(progress_drawable);
        markNew=root.findViewById(R.id.mark_new);
        setOnClickListeners(onItem,onItemButton);
        cover.setOnClickListener(v -> new AlertDialog.Builder(v.getContext()).setView(new AspectRatioPhotoView(v.getContext(),cover.getScaleType(),cover.getDrawable())).create().show());
    }
    public BookHolder(ViewGroup parent, HolderClickListener onItem, HolderClickListener onItemButton, boolean fullContent, boolean horizontal){
        this(parent,onItem,onItemButton);
        setFullContent(fullContent,horizontal);
    }
    public void bind(BookData data, boolean showSource, boolean showUpdated, long fullSize, boolean full_content, int orientation){
        setFullContent(full_content,orientation==RecyclerView.HORIZONTAL);
        bind(data,showSource,showUpdated,fullSize);
    }
    public void bind(BookData data, boolean showSource, boolean showCheckedNew, long fullSize){
        setCover(data.book);
        setTitle(data.title);
        setSubtitle(data.subtitle);
        setGenres(data.genres);
        setSize(data.size,fullSize);
        setDescriptionHint(data.description);
        setRating(data.rating);
        setNumSaved(data.saved);
        setNumNew(showCheckedNew?data.checked_new:data.not_checked_new);
        setButtonText(button.hasOnClickListeners() ? itemView.getContext().getString(R.string.CONTINUE) : (showSource ? data.source : data.status), !button.hasOnClickListeners() || (data.book.getNumChapterHistory()>=0));
        setVisibleMarkNew(data.not_checked_new>0);
    }

    public final void setFullContent(boolean full,boolean horizontal){
        this.cover.getLayoutParams().width=full ? cardWidth : ViewGroup.LayoutParams.MATCH_PARENT;
        this.cover.setRatioAndMode(full ? 1.5 : 1.3,horizontal ? AspectRatioImageView.Height : AspectRatioImageView.Width);
        this.full.setVisibility(full ? View.VISIBLE : View.GONE);
        this.titleShort.setVisibility(full ? View.INVISIBLE : View.VISIBLE);
        this.cover.setFocusable(full);
        this.cover.setClickable(full);
    }
    public final boolean isFull(){return this.full.getVisibility()!=View.GONE;}
    public final void setCover(Drawable drawable){
        this.cover.setScaleType(drawable==null ? ImageView.ScaleType.CENTER : ImageView.ScaleType.CENTER_CROP);
        this.cover.setImageDrawable(drawable==null ? caution : drawable);
        if(drawable instanceof Animatable animatable){
            animatable.start();
        }
    }
    public final void setCover(Book book){
        book.loadCover(this::setCover);
    }
    public final void setTitle(String title){
        this.titleShort.setText(title);
        this.title.setText(title);
    }
    public final void setSubtitle(String subtitle){
        this.subtitle.setText(subtitle);
    }
    public final void setGenres(String genres){
        this.genres.setText(genres);
    }
    public final void setRating(double rating){
        this.ratingBar.setRating(rating,true);
    }
    public final void setSizeText(String text,boolean show){
        size.setText(show?text:null);
    }
    public final void setButtonText(String text,boolean enabled){
        button.setText(text);
        button.setEnabled(enabled);
    }
    public final void setNumSaved(int num){
        num_saved.setText(String.valueOf(num));
        num_saved.setVisibility(num==0 ? View.INVISIBLE : View.VISIBLE);
    }
    public final void setNumNew(int num){
        num_new.setText(String.valueOf(num));
        num_new.setVisibility(num==0 ? View.INVISIBLE : View.VISIBLE);
    }
    public final void setSize(long size,long fullSize){
        setSizeText(Utils.File.SizeS(size),fullSize>0);
        setProgress(fullSize, size, Color.HSVToColor(new float[]{r.nextFloat()*360,1,1}));
    }
    public final void setVisibleMarkNew(boolean visible){
        markNew.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }
    public final void setProgress(int max,int progress){
        progress_drawable.setMax(max).setProgress(progress).invalidateSelf();
    }
    public final void setProgress(long max,long progress,int color){
        progress_drawable.setColors(color).setMax(max).setProgress(progress).invalidateSelf();
    }
    public final void setProgress(long max,long progress,int[] colors){
        progress_drawable.setColors(colors).setMax(max).setProgress(progress).invalidateSelf();
    }
    public final void setOnClickListeners(HolderClickListener onItem, HolderClickListener onItemButton){
        root.setOnClickListener(onItem==null?null:v->onItem.onItemClick(v,getBindingAdapterPosition()));
        button.setOnClickListener(onItemButton==null?null:v->onItemButton.onItemClick(v,getBindingAdapterPosition()));
        button.setClickable(onItemButton!=null);
    }
    public final void setDescriptionHint(String description){
        root.setOnLongClickListener(description==null || description.length()==0?null:v->{
            new SimpleTooltip.Builder(root.getContext())
                    .anchorView(root)
                    .contentView(R.layout.tooltip)
                    .text(description.strip())
                    .gravity(Gravity.BOTTOM)
                    .showArrow(false)
                    .animated(false)
                    .transparentOverlay(false)
                    .highlightShape(OverlayView.HIGHLIGHT_SHAPE_RECTANGULAR_ROUNDED)
                    .cornerRadius(root.getRadius()).overlayOffset(0).margin(0f)
                    .focusable(true)
                    .build()
                    .show();
            return true;
        });
    }
}
