package org.alex.kitsune.ui.preview;

import android.app.Activity;
import android.content.Intent;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.commons.ClickSpan;
import org.alex.kitsune.commons.HolderListener;
import org.alex.kitsune.commons.HolderMenuItemClickListener;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.book.Chapter;
import org.alex.kitsune.R;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.ui.search.AdvancedSearchActivity;
import java.util.Map;

import static org.alex.kitsune.Activity.animation;

public class ChapterHolder extends RecyclerView.ViewHolder {
    private final TextView title, subtitle,date,download;
    private final ImageView play,markNew,close;
    protected Book book;

    public ChapterHolder(ViewGroup parent, int layout, HolderListener listener, Book book, HolderMenuItemClickListener menuListener){
        super(LayoutInflater.from(parent.getContext()).inflate(layout,parent,false));
        if(listener!=null){itemView.setOnClickListener(v -> listener.onItemClick(v,getBindingAdapterPosition()));}
        if(listener!=null){itemView.setOnLongClickListener(v -> listener.onItemLongClick(v,getBindingAdapterPosition()));}
        title=itemView.findViewById(R.id.chapter_title);
        subtitle=itemView.findViewById(R.id.chapter_subtitle);
        subtitle.setMovementMethod(LinkMovementMethod.getInstance());
        date=itemView.findViewById(R.id.chapter_date);
        download=itemView.findViewById(R.id.download_text);
        play=itemView.findViewById(R.id.chapter_play);
        markNew=itemView.findViewById(R.id.mark_new);
        close=itemView.findViewById(R.id.chapter_close);
        this.book=book;
    }
    public void bind(Chapter chapter, boolean selected){
        title.setText(chapter.text(title.getContext()));
        if(chapter.getTranslators()!=null){
            SpannableStringBuilder str=new SpannableStringBuilder();
            boolean first=true;
            for(Map.Entry<String,String> entry:chapter.getTranslators().entrySet()){
                if(!first){str.append(", ");} first=false;
                if(entry.getValue()!=null){
                    str.append(entry.getKey(),new ClickSpan(entry.getValue(),(view, text)->view.getContext().startActivity(new Intent(view.getContext(),AdvancedSearchActivity.class).putExtra(Constants.catalog, book.getSource()).putExtra(Constants.title,entry.getKey()).putExtra(Constants.url,text!=null ? text.toString() : null),animation((Activity)view.getContext(),Gravity.START,Gravity.END))), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }else{
                    str.append(entry.getKey());
                }
            }
            subtitle.setText(str);
        }
        date.setText(getDate(chapter.getDate()));
        download.setVisibility(book.checkChapter(chapter) ? View.VISIBLE : View.GONE);
        play.setVisibility((book.getHistory()!=null && chapter.equals(book.getHistory().getChapter())) ? View.VISIBLE : View.INVISIBLE);
        markNew.setVisibility(book.isNew(chapter)? View.VISIBLE : View.INVISIBLE);
        close.setVisibility(chapter.isClose() && !book.checkChapter(chapter) ? View.VISIBLE : View.GONE);
        itemView.setActivated(selected);
    }
    public static java.text.SimpleDateFormat format=new java.text.SimpleDateFormat("dd.MM.yyyy",java.util.Locale.getDefault());
    public static String getDate(long date){return date>0 ? format.format(new java.util.Date(date)) : null;}
}