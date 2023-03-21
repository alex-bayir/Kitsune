package org.alex.kitsune.manga.views;

import android.view.ViewGroup;
import org.alex.kitsune.commons.DiffCallback;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.commons.HolderClickListener;
import org.alex.kitsune.commons.ListSet;
import org.alex.kitsune.manga.Manga;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class MangaAdapter extends RecyclerView.Adapter<MangaHolder> {
    public enum Mode{
        LIST,
        GRID,
        MIXED;
        Mode(){}
    }
    protected final ListSet<Manga> list=new ListSet<>(new ArrayList<>());
    private final HolderClickListener listener,buttonListener;
    private Mode mode;
    private boolean showSource;
    private boolean showCheckedNew=true;
    private long full_size;
    public MangaAdapter(Collection<Manga> mangas, Mode mode, Callback<Manga> clickListener){this(mangas,mode,clickListener,null);}
    public MangaAdapter(Collection<Manga> mangas, Mode mode, Callback<Manga> mangaListener, Callback<Manga> mangaButtonListener){
        if(mangas!=null){this.list.addAll(mangas);}
        this.mode=mode;
        this.listener=(v,position) -> mangaListener.call(list.get(position));
        this.buttonListener=mangaButtonListener!=null ? (v,position) -> mangaButtonListener.call(list.get(position)) : null;
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
    public MangaHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new MangaHolder(parent,listener,buttonListener,viewType==1,grid.getOrientation()==RecyclerView.HORIZONTAL);
    }

    @Override
    public void onBindViewHolder(MangaHolder holder, int position) {
        holder.bind(list.get(position),showSource, showCheckedNew, full_size);
    }

    @Override
    public int getItemCount(){return list.size();}

    public void sort(Comparator<Manga> comparator){sort(comparator,false);}
    public void sort(Comparator<Manga> comparator, boolean update){
        if(update){
            new DiffCallback<>(list, ()-> list.sort(comparator)).notifyUpdate(this);
        }else{
            list.sort(comparator);
        }
    }
    public void sort(Comparator<Manga> comparator, int spanCount){
        setSpanCount(spanCount,false);
        sort(comparator,true);
    }
    public void setShowCheckedNew(boolean showCheckedNew){this.showCheckedNew=showCheckedNew;}
    public boolean isShowCheckedNew(){return showCheckedNew;}
    public List<Manga> getList(){return this.list;}
    public Manga get(int index){return list.get(index);}
    public int get(Manga manga){return list.indexOf(manga);}
    public void replace(List<Manga> list, Comparator<Manga> comparator,boolean recalculateSize){
        if(comparator!=null){list.sort(comparator);}
        replace(list,recalculateSize);
    }
    public void replace(List<Manga> list){replace(list,false);}
    public void replace(List<Manga> list,boolean recalculateSize){
        new DiffCallback<>(this.list, () -> {this.list.clear(); this.list.addAll(list); if(recalculateSize){recalculateFullSize();}}).notifyUpdate(this);
    }
    public final boolean add(Manga manga){return add(list.size(),manga,false);}
    public boolean add(int pos,Manga manga){return add(pos,manga,true);}
    public boolean add(Manga manga, Comparator<Manga> comparator){return manga!=null && add(getPositionToInsert(list.listIterator(), comparator, manga),manga,true);}
    public boolean add(int pos,Manga manga,boolean moveIfExist){
        return manga!=null && notifyOnAddMoveChange(list.add(pos,manga,moveIfExist),pos,moveIfExist);
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
        return iterator.nextIndex();
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
    public boolean addAll(Collection<Manga> collection){
        int last=list.size();
        if(list.addAll(collection)){
            notifyItemRangeChanged(last,list.size()-last);
            return true;
        }
        return false;
    }
    public boolean addAll(Collection<Manga> collection, Comparator<Manga> comparator){
        return new DiffCallback<>(list, ()-> {
            list.addAll(collection);
            list.sort(comparator);
        }).notifyUpdate(this).getOldListSize()!=list.size();
    }
    public int update(Manga manga){
        int index=manga!=null ? get(manga) : -1;
        if(index!=-1){notifyItemChanged(index);}
        return index;
    }

    public Manga remove(int index){
        if(index>=0 && index<list.size()){
            Manga manga=list.remove(index);
            notifyItemRemoved(index);
            return manga;
        }else{
            return null;
        }
    }
    public Manga remove(Manga manga){
        return remove(get(manga));
    }
    public void clear(){
        notifyItemRangeRemoved(0,list.size());
        list.clear();
    }

    public void recalculateFullSize(){full_size=0; for(Manga manga:list){full_size+=manga.recalculateImagesSize();}}
    public void calculateFullSize(long offset){full_size=offset; for(Manga manga:list){full_size+=manga.getImagesSize();}}
    public void addBySize(Manga manga){
        calculateFullSize(manga.recalculateImagesSize());
        add(manga,Manga.ImagesSizesComparator);
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
