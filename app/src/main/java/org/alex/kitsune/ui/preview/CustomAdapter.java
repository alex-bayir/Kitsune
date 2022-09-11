package org.alex.kitsune.ui.preview;

import android.content.Context;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.DiffCallBack;
import org.alex.kitsune.commons.HolderListener;
import org.alex.kitsune.commons.HolderMenuItemClickListener;
import org.alex.kitsune.manga.BookMark;
import org.alex.kitsune.manga.Chapter;
import org.alex.kitsune.manga.Manga;
import java.util.List;

public class CustomAdapter <T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    final int ResourceId;
    List<T> items;
    HolderListener listener;
    HolderMenuItemClickListener menuListener;
    Manga manga;

    public CustomAdapter(Context context, Manga manga, List<T> list, int ResourceId, HolderListener listener, HolderMenuItemClickListener menuListener){
        this.context=context;
        this.items=list;
        this.ResourceId=ResourceId;
        this.listener=listener;
        this.menuListener=menuListener;
        this.manga=manga;
    }

    public void setManga(Manga manga){
        this.manga=manga;
    }
    public void setList(List<T> list){
        new DiffCallBack(items, list).updateAfter(()->items=list,this);
    }

    public List<T> getList(){return items;}

    @Override
    public int getItemViewType(int position){return ResourceId;}

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        switch (viewType){
            case R.layout.item_chapter: return new ChapterHolder(parent,viewType,listener,manga,menuListener);
            case R.layout.item_bookmark: return new BookMarkHolder(parent,viewType,listener,manga,menuListener);
            default: return null;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position){
        if(holder instanceof ChapterHolder){((ChapterHolder)holder).bind((Chapter)items.get(position));}
        if(holder instanceof BookMarkHolder){((BookMarkHolder)holder).bind((BookMark)items.get(position));}
    }

    @Override
    public int getItemCount(){return items.size();}

    public int getPosition(T item){
        if(item!=null && items!=null){
            for(int i=0;i<items.size();i++){
                if(item.equals(items.get(i))){return i;}
            }
        }
        return -1;
    }

    public int search(String query){
        for (Chapter chapter: manga.getChapters()) {
            if(chapter.text(context).contains(query)){return manga.getChapters().indexOf(chapter);}
        }
        return -1;
    }
}
