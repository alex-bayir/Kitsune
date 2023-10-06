package org.alex.kitsune.commons;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Objects;

public class DiffCallback<E> extends DiffUtil.Callback{
    protected List<E> o, n;
    private boolean check_contents_the_same;
    public DiffCallback(){
        this(false);
    }
    public DiffCallback(boolean check_contents_the_same){
        this.check_contents_the_same=check_contents_the_same;
    }
    public DiffCallback(List<E> o, List<E> n,boolean check_contents_the_same){
        init(o,n,check_contents_the_same);
    }
    public DiffCallback<E> init(List<E> o, List<E> n){
        return init(o,n,check_contents_the_same);
    }
    public DiffCallback<E> init(List<E> o, List<E> n,boolean check_contents_the_same){
        this.o=o;
        this.n=n;
        this.check_contents_the_same=check_contents_the_same;
        return this;
    }

    public boolean isCheckContentsTheSame(){
        return check_contents_the_same;
    }

    @Override public int getOldListSize(){return o.size();}
    @Override public int getNewListSize(){return n.size();}

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return check_contents_the_same && areItemsTheSame(o.get(oldItemPosition),n.get(newItemPosition));
    }
    public boolean areItemsTheSame(E o,E n){
        return Objects.equals(o,n);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return areContentsTheSame(o.get(oldItemPosition),n.get(newItemPosition));
    }
    public boolean areContentsTheSame(E o,E n){
        return Objects.hashCode(o)==Objects.hashCode(n);
    }

    public DiffUtil.DiffResult calculateDiff(){return calculateDiff(true);}
    public DiffUtil.DiffResult calculateDiff(boolean detectMoves){return o==null || n==null ? null : DiffUtil.calculateDiff(this, detectMoves);}
    public DiffCallback<E> notifyUpdate(final RecyclerView.Adapter<? extends RecyclerView.ViewHolder> adapter){
        return notifyUpdate(adapter,true);
    }
    public DiffCallback<E> notifyUpdate(final RecyclerView.Adapter<? extends RecyclerView.ViewHolder> adapter, boolean detectMoves){
        if(o!=null && n!=null){
            calculateDiff(detectMoves).dispatchUpdatesTo(adapter);
        }
        return this;
    }
}