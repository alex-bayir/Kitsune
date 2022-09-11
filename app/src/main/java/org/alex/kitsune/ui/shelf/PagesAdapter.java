package org.alex.kitsune.ui.shelf;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class PagesAdapter {
    FragmentManager fragmentManager;
    Shelf shelf;
    NewFragment n;
    StatisticsFragment s;
    int id;

    int current=-1;
    public PagesAdapter(FragmentManager fragmentManager,int containerId) {
        this.fragmentManager=fragmentManager;
        id=containerId;
    }

    public Fragment get(int position){
        switch(position){
            default: return shelf!=null ? shelf : (shelf=new Shelf());
            case 1: return n!=null ? n : (n=new NewFragment());
            case 2: return new Catalogs();
            case 3: return s!=null ? s : (s=new StatisticsFragment());
        }
    }
    public void setCurrentItem(int position){
        current=position;
        fragmentManager.beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).replace(id,get(position)).commit();
    }

    public Fragment getCurrent(){return get(current);}

    public int getItemCount(){return 4;}
}
