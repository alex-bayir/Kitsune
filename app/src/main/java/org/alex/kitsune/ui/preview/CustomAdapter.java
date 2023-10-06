package org.alex.kitsune.ui.preview;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.*;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.DiffCallback;
import org.alex.kitsune.commons.HolderListener;
import org.alex.kitsune.commons.HolderMenuItemClickListener;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.book.BookMark;
import org.alex.kitsune.book.Chapter;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class CustomAdapter <T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    final int ResourceId;
    List<T> items=new LinkedList<>();
    HolderListener listener;
    HolderMenuItemClickListener menuListener;
    Book book;
    SelectionTracker<Long> tracker;
    private boolean selectable=true;
    Function<Book,List<T>> getItems;
    private final DiffCallback<T> notify=new DiffCallback<>();

    public CustomAdapter(int holder_layout_id, HolderListener listener, HolderMenuItemClickListener menuListener,RecyclerView rv,String selectionId){
        this.ResourceId=holder_layout_id;
        this.getItems=switch (holder_layout_id) {
            case (R.layout.item_chapter) -> (book->(List<T>)book.getChapters());
            case (R.layout.item_bookmark) -> (book->(List<T>)book.getBookMarks());
            default -> throw new IllegalArgumentException("Wrong layout");
        };
        this.listener=listener;
        this.menuListener=menuListener;
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
    public CustomAdapter(Book book, int holder_layout_id, HolderListener listener, HolderMenuItemClickListener menuListener, RecyclerView rv, String selectionId){
        this(holder_layout_id, listener, menuListener, rv, selectionId);
        setBook(book);
    }

    public void setSelectable(boolean selectable){
        this.selectable=selectable;
    }
    public boolean isSelectable(){
        return selectable;
    }

    public void setBook(Book book){
        setList(getItems.apply(this.book=book));
    }
    private void setList(List<T> list){
        notify.init(items,items=new ArrayList<>(list)).notifyUpdate(this,true);
    }

    public List<T> getList(){return items;}

    @Override
    public long getItemId(int position){return Integer.toUnsignedLong(position);}
    @Override
    public int getItemViewType(int position){return ResourceId;}

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        return switch (viewType) {
            case (R.layout.item_chapter) -> new ChapterHolder(parent, viewType, listener, book, menuListener);
            case (R.layout.item_bookmark) -> new BookMarkHolder(parent, viewType, listener, book, menuListener);
            default -> throw new IllegalArgumentException("View holder cannot be null");
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position){
        if(holder instanceof ChapterHolder h){h.bind((Chapter)items.get(position),isSelected(position));}
        if(holder instanceof BookMarkHolder h){h.bind((BookMark)items.get(position));}
    }

    @Override
    public int getItemCount(){return items.size();}

    public boolean isSelected(int position){
        return tracker!=null&&tracker.isSelected(Integer.toUnsignedLong(position));
    }

    public int getPosition(T item){
        if(item!=null && items!=null){
            for(int i=0;i<items.size();i++){
                if(item.equals(items.get(i))){return i;}
            }
        }
        return -1;
    }

    public int search(Context context,String query){
        query=query.toLowerCase();
        for (Chapter chapter: book.getChapters()) {
            if(chapter.text(context).toLowerCase().contains(query)){return book.getChapters().indexOf(chapter);}
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
        public Long getKey(int position){return rv.getAdapter()!=null?rv.getAdapter().getItemId(position):-1;}

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
