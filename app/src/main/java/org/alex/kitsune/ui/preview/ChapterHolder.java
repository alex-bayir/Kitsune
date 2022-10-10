package org.alex.kitsune.ui.preview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.commons.HolderListener;
import org.alex.kitsune.commons.HolderMenuItemClickListener;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.manga.Chapter;
import org.alex.kitsune.R;

public class ChapterHolder extends RecyclerView.ViewHolder {
    private final TextView title, subtitle,date,download;
    private final ImageView play,markNew;
    protected Manga manga;

    public ChapterHolder(ViewGroup parent, int layout,HolderListener listener, Manga manga, HolderMenuItemClickListener menuListener){
        super(LayoutInflater.from(parent.getContext()).inflate(layout,parent,false));
        if(listener!=null){itemView.setOnClickListener(v -> listener.onItemClick(v,getBindingAdapterPosition()));}
        if(listener!=null){itemView.setOnLongClickListener(v -> listener.onItemLongClick(v,getBindingAdapterPosition()));}
        title=itemView.findViewById(R.id.chapter_title);
        subtitle=itemView.findViewById(R.id.chapter_subtitle);
        date=itemView.findViewById(R.id.chapter_date);
        download=itemView.findViewById(R.id.download_text);
        play=itemView.findViewById(R.id.chapter_play);
        markNew=itemView.findViewById(R.id.mark_new);
        this.manga=manga;
    }
    public void bind(Chapter chapter){
        title.setText(chapter.text(title.getContext()));
        subtitle.setText(chapter.getInfo());
        date.setText(getDate(chapter.getDate()));
        download.setVisibility(manga.checkChapter(chapter) ? View.VISIBLE : View.GONE);
        play.setVisibility((manga.getHistory()!=null && chapter.equals(manga.getHistory().getChapter())) ? View.VISIBLE : View.INVISIBLE);
        markNew.setVisibility(manga.isNew(chapter)? View.VISIBLE : View.INVISIBLE);
    }
    public static java.text.SimpleDateFormat format=new java.text.SimpleDateFormat("dd.MM.yyyy");
    public static String getDate(long date){return date>0 ? format.format(new java.util.Date(date)) : null;}
}