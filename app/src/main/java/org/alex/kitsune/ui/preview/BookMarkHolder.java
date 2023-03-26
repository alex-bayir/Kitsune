package org.alex.kitsune.ui.preview;

import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.commons.HolderListener;
import org.alex.kitsune.commons.HolderMenuItemClickListener;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.book.BookMark;
import org.alex.kitsune.R;

public class BookMarkHolder extends RecyclerView.ViewHolder{
    public static final int REMOVE=0;
    private final TextView date;
    private final ImageView image;
    private final AppCompatImageView menu;
    private final Drawable caution;
    private final PopupMenu popupMenu;
    protected Book book;

    public BookMarkHolder(ViewGroup parent, int layout, HolderListener listener, Book book, HolderMenuItemClickListener menuListener){
        super(LayoutInflater.from(parent.getContext()).inflate(layout,parent,false));
        if(listener!=null){itemView.setOnClickListener(v -> listener.onItemClick(v,getBindingAdapterPosition()));}
        if(listener!=null){itemView.setOnLongClickListener(v -> listener.onItemLongClick(v,getBindingAdapterPosition()));}
        image=itemView.findViewById(R.id.imageView);
        date=itemView.findViewById(R.id.textView);
        menu=itemView.findViewById(R.id.menu);
        caution=image.getDrawable();
        popupMenu=new PopupMenu(menu.getContext(), menu,Gravity.END,R.style.Widget_AppCompat_PopupMenu, R.style.Widget_AppCompat_PopupMenu);
        popupMenu.getMenu().add(0,REMOVE, Menu.NONE,R.string.remove).setIcon(R.drawable.ic_bookmark_remove_black).getIcon().setTint(0xffffffff);
        popupMenu.setOnMenuItemClickListener(item -> menuListener!=null && menuListener.onMenuItemClick(getBindingAdapterPosition(),item));
        popupMenu.setForceShowIcon(true);
        menu.setOnClickListener(v -> {if(v==menu){popupMenu.show();}});
        this.book=book;
    }


    public void bind(BookMark bookMark){
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImageDrawable(book.getPage(bookMark.getChapter(),bookMark.getPage()));
        if(image.getDrawable()==null){image.setImageDrawable(caution); image.setScaleType(ImageView.ScaleType.CENTER);}
        date.setText(getRelativeTime(bookMark.getDate()));
    }

    public static String getRelativeTime(long date){return DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(), 0).toString();}
}
