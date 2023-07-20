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
import java.util.function.Function;
import java.util.stream.Collectors;

public class BookAdapter extends RecyclerView.Adapter<BookHolder> {
    public enum Mode{
        LIST,
        GRID,
        MIXED;
        Mode(){}
    }
    protected final ListSet<BookData> data=new ListSet<>(new ArrayList<>());
    private List<BookData> old=new ArrayList<>();
    private final HolderClickListener holder, button;
    private Mode mode;
    private boolean showSource;
    private boolean showCheckedNew=true;
    private long full_size;
    private boolean enable_update=true;
    private DiffCallback<BookData> notify=new DiffCallback<>(){
        @Override
        public boolean areContentsTheSame(int old_pos, int new_pos) {
            return isCheckContentsTheSame() && BookData.areSame(o.get(old_pos),n.get(new_pos));
        }
    };
    public BookAdapter(Collection<Book> books, Mode mode, Callback<Book> clickListener){this(books,mode,clickListener,null);}
    public BookAdapter(Collection<Book> books, Mode mode, Callback<Book> holder, Callback<Book> button){
        if(books!=null){this.data.addAll(books.stream().map(BookData::new).collect(Collectors.toList())); calculateFullSize();}
        this.mode=mode;
        this.holder=(v, position) -> holder.call(data.get(position).book);
        this.button=button!=null ? (v, position) -> button.call(data.get(position).book) : null;
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
        return new BookHolder(parent, holder, button,viewType==1,grid.getOrientation()==RecyclerView.HORIZONTAL);
    }

    @Override
    public void onBindViewHolder(BookHolder holder, int position) {
        holder.bind(data.get(position),showSource, showCheckedNew, full_size);
    }

    @Override
    public int getItemCount(){return data.size();}
    public void sort(Comparator<Book> comparator){
        notify(data.stream().map(BookData::update).sorted((o1, o2) -> comparator.compare(o1.book,o2.book)).collect(Collectors.toList()));
    }
    public void sort(Comparator<Book> comparator, int spanCount){
        setSpanCount(spanCount,false);
        sort(comparator);
    }
    public void setShowCheckedNew(boolean showCheckedNew){this.showCheckedNew=showCheckedNew;}
    public boolean isShowCheckedNew(){return showCheckedNew;}
    public List<Book> getList(){return this.data.stream().map(d->d.book).collect(Collectors.toList());}
    public Book get(int index){return data.get(index).book;}
    public int get(Book book){
        return data.indexOf(book!=null?new BookData(book):null);
    }
    public void replace(List<Book> list, Comparator<Book> comparator, boolean recalculateSize){
        List<BookData> updated=list.stream().map(book -> new BookData(book,recalculateSize)).sorted(BookData.convert(comparator)).collect(Collectors.toList());
        if(recalculateSize){calculateFullSize(updated);}
        notify(updated);
    }
    public void replace(List<Book> list){replace(list,false);}
    public void replace(List<Book> list, boolean recalculateSize){
        List<BookData> updated=list.stream().map(book -> new BookData(book,recalculateSize)).collect(Collectors.toList());
        if(recalculateSize){calculateFullSize(updated);}
        notify(updated);
    }
    public final void add(Book book){add(data.size(), book,false);}
    public void add(Book book, Comparator<Book> comparator){
        add(((o1, o2) -> comparator.compare(o1.book,o2.book)),book);
    }
    private void add(Comparator<BookData> comparator, Book book){
        if(book!=null){
            notify(data->{data.add(new BookData(book),true); data.sort(comparator); return data;});
        }
    }
    public void add(int pos, Book book, boolean moveIfExist){
        if(book!=null){
            notify(data->{data.add(pos,new BookData(book),moveIfExist); return data;});
        }
    }
    public void addAll(Collection<Book> collection){
        notify(data->{data.addAll(collection.stream().map(BookData::new).collect(Collectors.toList())); return data;});
    }
    public void addAll(Collection<Book> collection, Comparator<Book> comparator){
        notify(data->{data.addAll(collection.stream().map(BookData::new).sorted(((o1, o2) -> comparator.compare(o1.book,o2.book))).collect(Collectors.toList())); return data;});
    }
    public int update(Book book){
        if(book==null){return -1;}
        BookData data=new BookData(book);
        int index=this.data.indexOf(data);
        if(index!=-1 && !BookData.areSame(data,this.data.get(index))){
            add(index,book,true);
        }
        return index;
    }

    public Book remove(int index){
        if(index>=0 && index<data.size()){
            List<BookData> old=new ArrayList<>(data);
            Book book=data.remove(index).book;
            notify(old,data);
            return book;
        }else{
            return null;
        }
    }
    public void remove(Book book){
        remove(get(book));
    }
    public void clear(){
        notifyItemRangeRemoved(0,data.size());
        data.clear();
    }

    public void calculateFullSize(){calculateFullSize(data);}
    private void calculateFullSize(List<BookData> list){full_size=list.stream().mapToLong(data->data.size).sum();}

    public void notifyAllChanged(){notifyItemRangeChanged(0,getItemCount());}
    public void notify(List<BookData> n){
        notify(new ArrayList<>(data), n);
    }
    public void notify(Function<ListSet<BookData>,ListSet<BookData>> f){
        notify(new ArrayList<>(data),f.apply(data));
    }
    public void notify(List<BookData> o, List<BookData>n){
        if(o!=null){
            if(n!=data){data.clear(); data.addAll(n);}
            if(enable_update){
                notify.init(o,n, BookData.areSameBooks(o,n)).notifyUpdate(this);
            }
        }
    }
    public void setEnableUpdate(boolean enable_update){
        if(this.enable_update!=enable_update){
            this.enable_update=enable_update;
            if(enable_update){
                notify(old,data);
            }
            old=enable_update?null:new ArrayList<>(data);
        }
    }

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
