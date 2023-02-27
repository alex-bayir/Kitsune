package org.alex.kitsune.ui.preview;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.*;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.DiffCallBack;
import org.alex.kitsune.commons.HolderListener;
import org.alex.kitsune.commons.HolderMenuItemClickListener;
import org.alex.kitsune.manga.BookMark;
import org.alex.kitsune.manga.Chapter;
import org.alex.kitsune.manga.Manga;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class CustomAdapter <T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    final int ResourceId;
    List<T> items;
    HolderListener listener;
    HolderMenuItemClickListener menuListener;
    Manga manga;
    SelectionTracker<Long> tracker;
    private boolean selectable=true;

    public CustomAdapter(Context context, Manga manga, List<T> list, int ResourceId, HolderListener listener, HolderMenuItemClickListener menuListener,RecyclerView rv,String selectionId){
        this.context=context;
        this.items=list;
        this.ResourceId=ResourceId;
        this.listener=listener;
        this.menuListener=menuListener;
        this.manga=manga;
        setHasStableIds(true);
        rv.setAdapter(this);
        if(selectionId!=null){
            tracker=new SelectionTracker.Builder<>(selectionId, rv, new KeyProvider(rv), new ItemDetailsLookup(rv), StorageStrategy.createLongStorage())
                    .withSelectionPredicate(new SelectionTracker.SelectionPredicate<>() {
                        @Override public boolean canSetStateForKey(@NonNull @NotNull Long key, boolean nextState) {return selectable;}
                        @Override public boolean canSetStateAtPosition(int position, boolean nextState) {return true;}
                        @Override public boolean canSelectMultiple() {return true;}
                    }).build();
        }
    }

    public void setSelectable(boolean selectable){
        this.selectable=selectable;
    }
    public boolean isSelectable(){
        return selectable;
    }

    public void setManga(Manga manga){
        this.manga=manga;
    }
    public void setList(List<T> list){
        new DiffCallBack(items, list).updateAfter(()->items=list,this);
    }

    public List<T> getList(){return items;}

    @Override
    public long getItemId(int position){return Integer.toUnsignedLong(position);}
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
        boolean selected=tracker!=null&&tracker.isSelected(Integer.toUnsignedLong(position));
        if(holder instanceof ChapterHolder){((ChapterHolder)holder).bind((Chapter)items.get(position),selected);}
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
    public SelectionTracker<Long> getTracker(){return tracker;}

    public static class KeyProvider extends ItemKeyProvider<Long>{
        RecyclerView rv;
        public KeyProvider(RecyclerView rv){
            super(ItemKeyProvider.SCOPE_MAPPED);
            this.rv=rv;
        }
        @Nullable
        @org.jetbrains.annotations.Nullable
        @Override
        public Long getKey(int position){return rv.getAdapter().getItemId(position);}

        @Override
        public int getPosition(@NonNull @NotNull Long key) {
            RecyclerView.ViewHolder holder=rv.findViewHolderForItemId(key);
            return holder!=null?holder.getLayoutPosition():RecyclerView.NO_POSITION;
        }
    }

    public static class ItemDetailsLookup extends androidx.recyclerview.selection.ItemDetailsLookup<Long> {
        RecyclerView rv;
        public ItemDetailsLookup(RecyclerView rv){
            this.rv=rv;
        }

        public ItemDetails<Long> getItemDetails(RecyclerView.ViewHolder holder){
            return new ItemDetails<>() {
                @Override
                public int getPosition() {
                    return holder.getBindingAdapterPosition();
                }
                @Override
                public Long getSelectionKey() {
                    return Integer.toUnsignedLong(holder.getBindingAdapterPosition());
                }
            };
        }
        @Override
        public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
            android.view.View view=rv.findChildViewUnder(e.getX(),e.getY());
            return view!=null ? getItemDetails(rv.getChildViewHolder(view)) : null;
        }
    }
}
