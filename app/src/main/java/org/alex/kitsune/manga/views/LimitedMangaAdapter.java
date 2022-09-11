package org.alex.kitsune.manga.views;

import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.manga.Manga;
import java.util.Collection;

public class LimitedMangaAdapter extends MangaAdapter {
    private int maxCount;
    public LimitedMangaAdapter(Collection<Manga> list, Mode mode, int maxCount, Callback<Manga> listener) {
        super(list, mode, listener);
        this.maxCount=Math.max(maxCount,0);
    }

    public LimitedMangaAdapter(Collection<Manga> list, Mode mode, int maxCount, Callback<Manga> listener, Callback<Manga> buttonListener) {
        super(list, mode, listener, buttonListener);
        this.maxCount=Math.max(maxCount,0);
    }
    public void setMaxCount(int maxCount){
        this.maxCount=maxCount;
    }

    @Override
    public int getItemCount() {
        return Math.min(super.getItemCount(),maxCount);
    }

    public int getMaxCount(){return this.maxCount;}

    @Override
    protected boolean notifyOnAddMoveChange(int old, int pos, boolean moveIfExist) {
        if(!moveIfExist && old!=-1){pos=old;}
        if(old!=pos){
            if(old<getItemCount() && old>=0){
                if(pos<getItemCount() && pos>=0){
                    notifyItemMoved(old,pos);
                    notifyItemChanged(pos);
                }else{
                    notifyItemRemoved(old);
                }
            }else{
                if(pos<getItemCount() && pos>=0){
                    if(list.size()>maxCount){notifyItemRemoved(getItemCount()-1);}
                    notifyItemInserted(pos);
                }else{
                    //nothing
                }
            }
        }else{
            notifyItemChanged(pos);
        }
        return old!=pos && moveIfExist;
    }
}
