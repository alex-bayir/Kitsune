package org.alex.kitsune.manga.views;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import org.alex.kitsune.R;
import com.alex.ratingbar.RatingBar;
import org.alex.kitsune.commons.AspectRatioImageView;
import org.alex.kitsune.commons.HolderClickListener;
import org.alex.kitsune.commons.ProgressDrawable;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.utils.Utils;
import java.util.Random;


public class MangaHolder extends RecyclerView.ViewHolder{

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

    public MangaHolder(ViewGroup parent, HolderClickListener onItem, HolderClickListener onItemButton){
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manga,parent,false));
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
        if(onItem!=null){
            root.setOnClickListener(v->onItem.onItemClick(v,getBindingAdapterPosition()));
        }
        if(onItemButton!=null){
            button.setOnClickListener(v->onItemButton.onItemClick(v,getBindingAdapterPosition()));
        }else{
            button.setClickable(false);
            button.setFocusable(false);
        }
        cover.setOnClickListener(v -> new AlertDialog.Builder(v.getContext()).setView(new AspectRatioImageView(v.getContext(),cover.getScaleType(),cover.getDrawable())).create().show());
    }
    public MangaHolder(ViewGroup parent, HolderClickListener onItem, HolderClickListener onItemButton, boolean fullContent, boolean horizontal){
        this(parent,onItem,onItemButton);
        setFullContent(fullContent,horizontal);
    }

    public void bind(Manga manga, boolean showSource, boolean showUpdated, long fullSize){
        setCover(manga);
        setTitle(manga.getName());
        setSubtitle(manga.getNameAlt());
        setGenres(manga.getGenres());
        setSize(manga.getImagesSize(),fullSize);
        setRating(manga.getRating());
        setNumSaved(manga.countSaved());
        setNumNew(showUpdated ? manga.getNotCheckedNew() : manga.getCheckedNew());
        setButtonText(button.hasOnClickListeners() ? itemView.getContext().getString(R.string.CONTINUE) : (showSource ? manga.getProviderName() : manga.getStatus()), !button.hasOnClickListeners() || (manga.getNumChapterHistory()>=0));
        setVisibleMarkNew(manga.getNotCheckedNew()>0);
    }
    public void bind(Manga manga, boolean showSource, boolean showUpdated, long fullSize, boolean full_content, int orientation){
        setFullContent(full_content,orientation==RecyclerView.HORIZONTAL);
        bind(manga,showSource,showUpdated,fullSize);
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
    public final void setCover(Drawable d){
        this.cover.setScaleType(d==null ? ImageView.ScaleType.CENTER : ImageView.ScaleType.CENTER_CROP);
        this.cover.setImageDrawable(d==null ? caution : d);
    }
    public final void setCover(Manga manga){
        manga.loadThumbnail(this::setCover);
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
        size.setText(text);
        size.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }
    public final void setButtonText(String text,boolean enabled){
        button.setText(text);
        button.setEnabled(enabled);
    }
    public final void setNumSaved(int num){
        num_saved.setText(Integer.toString(num));
        num_saved.setVisibility(num==0 ? View.INVISIBLE : View.VISIBLE);
    }
    public final void setNumNew(int num){
        num_new.setText(Integer.toString(num));
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
        if(onItem!=null){
            root.setOnClickListener(v->onItem.onItemClick(v,getBindingAdapterPosition()));
        }
        if(onItemButton!=null){
            button.setOnClickListener(v->onItemButton.onItemClick(v,getBindingAdapterPosition()));
        }else{
            button.setClickable(false);
            button.setFocusable(false);
        }
    }
}
