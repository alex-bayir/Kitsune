package org.alex.kitsune.commons;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Objects;

public class DiffCallBack extends DiffUtil.Callback{
    public List<?> o,n;
    public DiffCallBack(List<?> o, List<?> n){this.o=o; this.n=n;}
    @Override public int getOldListSize(){return o.size();}
    @Override public int getNewListSize(){return n.size();}

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return Objects.equals(o.get(oldItemPosition), n.get(newItemPosition));
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return false;
    }
    public void updateAfter(Runnable before, RecyclerView.Adapter adapter){
        if(before!=null){before.run();}
        DiffUtil.calculateDiff(this).dispatchUpdatesTo(adapter);
    }
}
