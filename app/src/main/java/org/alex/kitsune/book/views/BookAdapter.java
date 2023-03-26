package org.alex.kitsune.book.views;

import android.view.ViewGroup;
import org.alex.kitsune.commons.DiffCallback;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.commons.HolderClickListener;
import org.alex.kitsune.commons.ListSet;
import org.alex.kitsune.book.Book;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class BookAdapter extends RecyclerView.Adapter<BookHolder> {
    public enum Mode{
        LIST,
        GRID,
        MIXED;
        Mode(){}
    }
    protected final ListSet<Book> list=new ListSet<>(new ArrayList<>());
    private final HolderClickListener listener,buttonListener;
    private Mode mode;
    private boolean showSource;
    private boolean showCheckedNew=true;
    private long full_size;
    public BookAdapter(Collection<Book> books, Mode mode, Callback<Book> clickListener){this(books,mode,clickListener,null);}
    public BookAdapter(Collection<Book> books, Mode mode, Callback<Book> holder, Callback<Book> button){
        if(books !=null){this.list.addAll(books);}
        this.mode=mode;
        this.listener=(v,position) -> holder.call(list.get(position));
        this.buttonListener=button!=null ? (v,position) -> button.call(list.get(position)) : null;
    }
    private GridLayoutManager grid;
    public void setLayoutManager(GridLayoutManager grid){this.grid=grid; if(grid!=null){setOrientation(grid.getOrientation()); if(this.mode!=Mode.MIXED && grid.getOrientation()==RecyclerView.VERTICAL){setSpanCount(grid.getSpanCount());}}}
    public GridLayoutManager getLayoutManager(){return grid;}
    public void setSpanCount(int spanCount){setSpanCount(spanCount,true);}
    public void setSpanCount(int spanCount, boolean update){setSpanCount(spanCount,spanCount!=1 ? (mode!=Mode.LIST ? mode : Mode.GRID) : Mode.LIST, update);}
    public void setSpanCount(int spanCount, Mode mode){setSpanCount(spanCount, mode, true);}
    public void setSpanCount(int spanCount, Mode mode, boolean update){
        grid.setSpanCount(spanCount);
        setViewMode(mode, update);
    }
    public Mode getMode(){return mode;}
    public void setViewMode(Mode mode){setViewMode(mode,true);}
    public void setViewMode(Mode mode, boolean update){
        this.mode=mode; if(update){notifyAllChanged();}
    }

    public void setShowSource(boolean showSource){setShowSource(showSource,true);}
    public void setShowSource(boolean showSource, boolean update){
        this.showSource=showSource; if(update){notifyAllChanged();}
    }
    public boolean isShowSource(){return this.showSource;}
    public void setOrientation(int orientation){
        grid.setOrientation(orientation);
    }

    @Override
    public int getItemViewType(int position) {
        return (mode==Mode.LIST || (mode==Mode.MIXED && position==0)) ? 1:0;
    }

    @NotNull
    @Override
    public BookHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new BookHolder(parent,listener,buttonListener,viewType==1,grid.getOrientation()==RecyclerView.HORIZONTAL);
    }

    @Override
    public void onBindViewHolder(BookHolder holder, int position) {
        holder.bind(list.get(position),showSource, showCheckedNew, full_size);
    }

    @Override
    public int getItemCount(){return list.size();}

    public void sort(Comparator<Book> comparator){sort(comparator,false);}
    public void sort(Comparator<Book> comparator, boolean update){
        if(update){
            new DiffCallback<>(list, ()-> list.sort(comparator)).notifyUpdate(this);
        }else{
            list.sort(comparator);
        }
    }
    public void sort(Comparator<Book> comparator, int spanCount){
        setSpanCount(spanCount,false);
        sort(comparator,true);
    }
    public void setShowCheckedNew(boolean showCheckedNew){this.showCheckedNew=showCheckedNew;}
    public boolean isShowCheckedNew(){return showCheckedNew;}
    public List<Book> getList(){return this.list;}
    public Book get(int index){return list.get(index);}
    public int get(Book book){return list.indexOf(book);}
    public void replace(List<Book> list, Comparator<Book> comparator, boolean recalculateSize){
        if(comparator!=null){list.sort(comparator);}
        replace(list,recalculateSize);
    }
    public void replace(List<Book> list){replace(list,false);}
    public void replace(List<Book> list, boolean recalculateSize){
        new DiffCallback<>(this.list, () -> {this.list.clear(); this.list.addAll(list); if(recalculateSize){recalculateFullSize();}}).notifyUpdate(this);
    }
    public final boolean add(Book book){return add(list.size(), book,false);}
    public boolean add(int pos, Book book){return add(pos, book,true);}
    public boolean add(Book book, Comparator<Book> comparator){return book!=null && add(getPositionToInsert(list.listIterator(), comparator, book), book,true);}
    public boolean add(int pos, Book book, boolean moveIfExist){
        return book!=null && notifyOnAddMoveChange(list.add(pos, book,moveIfExist),pos,moveIfExist);
    }
    public <E> int getPositionToInsert(ListIterator<E> iterator, Comparator<E> comparator, E e){
        boolean f=false;
        while (iterator.hasNext()){
            int c=comparator.compare(e,iterator.next());
            if(c<0){
                return iterator.nextIndex()-(f ? 2:1);
            }else if(c==0){
                f=true;
            }
        }
        return iterator.nextIndex()-(f ? 1:0);
    }

    protected boolean notifyOnAddMoveChange(int old, int pos, boolean moveIfExist){
        if(old==-1){
            notifyItemInserted(pos);
        }else{
            if(!moveIfExist){pos=old;}
            if(old!=pos){notifyItemMoved(old,pos);}
            notifyItemChanged(pos);
        }
        return old!=pos && moveIfExist;
    }
    public boolean addAll(Collection<Book> collection){
        int last=list.size();
        if(list.addAll(collection)){
            notifyItemRangeChanged(last,list.size()-last);
            return true;
        }
        return false;
    }
    public boolean addAll(Collection<Book> collection, Comparator<Book> comparator){
        return new DiffCallback<>(list, ()-> {
            list.addAll(collection);
            list.sort(comparator);
        }).notifyUpdate(this).getOldListSize()!=list.size();
    }
    public int update(Book book){
        int index= book !=null ? get(book) : -1;
        if(index!=-1){notifyItemChanged(index);}
        return index;
    }

    public Book remove(int index){
        if(index>=0 && index<list.size()){
            Book book =list.remove(index);
            notifyItemRemoved(index);
            return book;
        }else{
            return null;
        }
    }
    public Book remove(Book book){
        return remove(get(book));
    }
    public void clear(){
        notifyItemRangeRemoved(0,list.size());
        list.clear();
    }

    public void recalculateFullSize(){full_size=0; for(Book book :list){full_size+= book.recalculateImagesSize();}}
    public void calculateFullSize(long offset){full_size=offset; for(Book book :list){full_size+= book.getImagesSize();}}
    public void addBySize(Book book){
        calculateFullSize(book.recalculateImagesSize());
        add(book, Book.ImagesSizesComparator);
        notifyAllChanged();
    }
    public void notifyAllChanged(){notifyItemRangeChanged(0,getItemCount());}

    public void setSpanChanger(RecyclerView rv){
        rv.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if(mode==Mode.LIST){
                setSpanCount(Math.max(1,((right-left)/Math.min(right-left,bottom-top))), mode);
            }
        });
    }
    public void initRV(RecyclerView rv, int spanCount){
        initRV(rv,spanCount,RecyclerView.VERTICAL,false);
    }
    public void initRV(RecyclerView rv, int spanCount, int orientation, boolean reverseLayout){
        initRV(rv,new GridLayoutManager(rv.getContext(),spanCount,orientation,reverseLayout));
    }
    public void initRV(RecyclerView rv, GridLayoutManager layoutManager){
        setLayoutManager(layoutManager);
        rv.setLayoutManager(getLayoutManager());
        if(getMode()!=Mode.LIST){
            setViewMode(getLayoutManager().getSpanSizeLookup().getSpanSize(0)==1?Mode.GRID:Mode.MIXED,false);
        }
        rv.setAdapter(this);
    }

    public static GridLayoutManager create(android.content.Context context, int spanCount){return create(context,spanCount, null);}
    public static GridLayoutManager create(android.content.Context context, int spanCount, java.util.function.Function<Integer,Integer> lookup){
        GridLayoutManager grid=new GridLayoutManager(context,spanCount);
        if(lookup!=null){
            grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup(){@Override public int getSpanSize(int position){return lookup.apply(position);}});
        }
        return grid;
    }
}
