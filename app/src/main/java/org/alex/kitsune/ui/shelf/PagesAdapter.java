package org.alex.kitsune.ui.shelf;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import static org.alex.kitsune.Activity.cloneTransition;

public class PagesAdapter {
    Activity activity;
    FragmentManager fragmentManager;
    Shelf shelf;
    NewFragment n;
    StatisticsFragment s;
    int id;

    int current=-1;
    public PagesAdapter(Activity activity,FragmentManager manager,int containerId){
        this.activity=activity;
        this.fragmentManager=manager;
        this.id=containerId;
    }

    public Fragment get(int position){
        return switch(position){
            default -> shelf!=null ? shelf : (shelf=new Shelf());
            case 1 -> n!=null ? n : (n=new NewFragment());
            case 2 -> new Catalogs();
            case 3 -> s!=null ? s : (s=new StatisticsFragment());
        };
    }
    public void setCurrentItem(int position){
        current=position;
        Fragment fragment=get(position);
        if(org.alex.kitsune.Activity.isAnimationsEnable()){
            fragment.setEnterTransition(cloneTransition(activity.getWindow().getEnterTransition()));
            fragment.setExitTransition(cloneTransition(activity.getWindow().getExitTransition()));
            fragment.setReturnTransition(cloneTransition(activity.getWindow().getReturnTransition()));
            fragment.setReenterTransition(cloneTransition(activity.getWindow().getReenterTransition()));
            fragmentManager.beginTransaction().replace(id,fragment).commit();
        }else{
            fragmentManager.beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).replace(id,fragment).commit();
        }
    }

    public Fragment getCurrent(){return get(current);}
    public int getCurrentIndex(){return current;}

    public int getItemCount(){return 4;}
}
