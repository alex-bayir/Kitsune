package org.alex.kitsune.commons;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DiffCallback<E> extends DiffUtil.Callback{
    List<E> o, n; int o_size, n_size;
    public DiffCallback(List<E> o, List<E> n){
        this(o,o.size(),n,n.size());
    }
    public DiffCallback(List<E> o, int o_size, List<E> n, int n_size){
        this.o=o; this.o_size = o_size;
        this.n=n; this.n_size = n_size;
    }
    public DiffCallback(List<E> o, Runnable callback){
        this.o=new ArrayList<>(o); this.n=o; callback.run();
        this.o_size=this.o.size(); this.n_size=this.n.size();
    }

    @Override public int getOldListSize(){return o_size;}
    @Override public int getNewListSize(){return n_size;}

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return Objects.equals(o.get(oldItemPosition), n.get(newItemPosition));
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return false;
    }

    public DiffUtil.DiffResult calculateDiff(){return DiffUtil.calculateDiff(this);}
    public DiffUtil.DiffResult calculateDiff(boolean detectMoves){return DiffUtil.calculateDiff(this, detectMoves);}
    public DiffCallback<E> notifyUpdate(final RecyclerView.Adapter adapter){
        calculateDiff().dispatchUpdatesTo(adapter);
        return this;
    }
    public DiffCallback<E> notifyUpdate(final RecyclerView.Adapter adapter, boolean detectMoves){
        calculateDiff(detectMoves).dispatchUpdatesTo(adapter);
        return this;
    }
    public void updateAfter(Runnable before, RecyclerView.Adapter adapter){
        if(before!=null){before.run();}
        notifyUpdate(adapter,true);
    }
}
