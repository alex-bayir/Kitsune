package org.alex.kitsune.ui.shelf;

import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.ui.main.MainActivity;
import org.alex.kitsune.services.MangaService;
import org.alex.kitsune.manga.views.LimitedMangaAdapter;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.R;
import org.alex.kitsune.manga.views.MangaAdapter;
import org.alex.kitsune.commons.MultiSelectListPreference;
import org.alex.kitsune.ui.preview.PreviewActivity;
import org.alex.kitsune.ui.reader.ReaderActivity;
import org.alex.kitsune.ui.shelf.favorite.FavoritesActivity;
import org.alex.kitsune.ui.shelf.history.HistoryActivity;
import org.alex.kitsune.ui.shelf.saved.SavedActivity;
import org.alex.kitsune.utils.NetworkUtils;
import org.jetbrains.annotations.NotNull;
import java.util.*;


public class Shelf extends Fragment implements MenuProvider {
    LinearLayout l;
    LimitedMangaAdapter adapterH,adapterS;
    Hashtable<String,LimitedMangaAdapter> adaptersF;
    Hashtable<MangaAdapter,View> views;
    HashSet<String> categoriesShowing;
    SharedPreferences prefs;
    public SmoothProgressBar progress;
    private int p=0;
    View root;
    MainActivity mainActivity;
    private final LinkedList<Runnable> tasks=new LinkedList<>();
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        requireActivity().removeMenuProvider(this);
        requireActivity().addMenuProvider(this);
        if(root==null){root=inflater.inflate(R.layout.content_shelf,container,false); mainActivity=(MainActivity)getActivity();}else{return root;}
        progress=mainActivity.findViewById(R.id.progress);
        progress.setIndeterminateDrawable(new SmoothProgressDrawable.Builder(progress.getContext()).colors(new int[]{0xff0000ff,0xff00ffff,0xff00ff00,0xffffff00,0xffff0000,0xffff00ff}).interpolator(new AccelerateDecelerateInterpolator()).callbacks(new SmoothProgressDrawable.Callbacks() {
            @Override public void onStop(){progress.setVisibility(View.GONE);}
            @Override public void onStart(){progress.setVisibility(View.VISIBLE);}
        }).build());
        progress.progressiveStop();
        prefs=PreferenceManager.getDefaultSharedPreferences(getContext());
        l=root.findViewById(R.id.linear_layout);
        update(inflater);
        IntentFilter filter=new IntentFilter(Constants.action_Update);
        filter.addAction(Constants.action_Update_Shelf);
        getContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(Constants.action_Update.equals(intent.getAction())){
                    Manga manga=MangaService.get(intent.getIntExtra(Constants.hash,-1));
                    if(manga!=null){
                        String tmp=intent.getStringExtra(Constants.option);
                        switch(tmp!=null ? tmp : ""){
                            case Constants.history:
                                addViewIfAdapter(l,views,adapterH,0);
                                adapterH.add(0,manga);
                                adapterS.update(manga);
                                for(Map.Entry<String,LimitedMangaAdapter> entry: adaptersF.entrySet()){
                                    entry.getValue().update(manga);
                                }
                                break;
                            case Constants.load:
                                adapterH.update(manga);
                                addViewIfAdapter(l,views,adapterS,-1);
                                adapterS.add(0,manga);
                                for(Map.Entry<String,LimitedMangaAdapter> entry: adaptersF.entrySet()){
                                    entry.getValue().update(manga);
                                }
                                break;
                            case Constants.favorites:
                                adapterH.update(manga);
                                adapterS.update(manga);
                                for(Map.Entry<String,LimitedMangaAdapter> entry: adaptersF.entrySet()){
                                    if(entry.getKey().equals(manga.getCategoryFavorite())){
                                        addViewIfAdapter(l,views,entry.getValue(),checkViewInChildren(l,views.get(adapterH)) ? 1 : 0);
                                        entry.getValue().add(0,manga);
                                    }else{
                                        entry.getValue().remove(manga);
                                        removeViewIfAdapter(l,views,entry.getValue());
                                    }
                                }
                                if(manga.getCategoryFavorite()!=null && !adaptersF.containsKey(manga.getCategoryFavorite())){
                                    String key=manga.getCategoryFavorite();
                                    l.addView(createView(createAdapter(MangaService.Type.Favorites,key),new GridLayoutManager(getContext(),3),key, v -> startActivity(new Intent(getContext(), FavoritesActivity.class).putExtra(Constants.category,key)),LayoutInflater.from(getContext()),l),checkViewInChildren(l,views.get(adapterH)) ? 1 : 0);
                                }
                                break;
                            default:
                                if(manga.getHistory()!=null){
                                    adapterH.update(manga);
                                }else{
                                    adapterH.remove(manga);
                                    removeViewIfAdapter(l,views,adapterH);
                                }
                                if(manga.countSaved()>0){
                                    adapterS.update(manga);
                                }else{
                                    adapterS.remove(manga);
                                    removeViewIfAdapter(l,views,adapterS);
                                }
                                for(Map.Entry<String,LimitedMangaAdapter> entry: adaptersF.entrySet()){
                                    entry.getValue().update(manga);
                                }
                            break;
                        }
                    }
                }
                if(Constants.action_Update_Shelf.equals(intent.getAction())){
                    if(getActivity()!=null){
                        update(getLayoutInflater());
                    }else{tasks.add(()->update(getLayoutInflater()));}
                }
            }
        },filter);
        return root;
    }

    private void update(LayoutInflater inflater){
        l.removeAllViews();
        views=new Hashtable<>();
        createAdapter(MangaService.Type.History,null);
        createView(adapterH,MangaAdapter.create(getContext(), 4, position->position==0 ? 4:1),getString(R.string.History), v -> startActivity(new Intent(getContext(), HistoryActivity.class)),inflater, l);
        addViewIfAdapter(l,views,adapterH,0);

        adaptersF=new Hashtable<>();
        if(prefs.getInt(Constants.favoritesRows,4)>0){
            for(String key:categoriesShowing=getShowingFavoriteCategories(prefs)){
                createView(createAdapter(MangaService.Type.Favorites,key),MangaAdapter.create(getContext(),3),key, v -> startActivity(new Intent(getContext(), FavoritesActivity.class).putExtra(Constants.category,key)),LayoutInflater.from(getContext()),l);
                addViewIfAdapter(l,views,adaptersF.get(key),-1);
            }
        }

        createAdapter(MangaService.Type.Saved,null);
        createView(adapterS,MangaAdapter.create(getContext(),4),getString(R.string.Saved), v -> startActivity(new Intent(getContext(), SavedActivity.class)),inflater, l);
        addViewIfAdapter(l,views,adapterS,-1);
    }

    @Override
    public void onCreateMenu(@NonNull @NotNull Menu menu, @NonNull @NotNull MenuInflater menuInflater) {}

    @Override
    public boolean onMenuItemSelected(@NonNull @NotNull MenuItem menuItem) {
        switch(menuItem.getItemId()){
            case R.id.check_for_updates: check_for_updates(); return true;
        }
        return false;
    }

    @Override
    public void onPrepareMenu(@NonNull @NotNull Menu menu) {
        menu.findItem(R.id.action_find_manga).setVisible(true);
        menu.findItem(R.id.check_for_updates).setVisible(!MangaService.isAllUpdated()).setEnabled(!MangaService.isUpdating);
        menu.findItem(R.id.action_add_source).setVisible(false);
        menu.findItem(R.id.full).setVisible(false);
        menu.findItem(R.id.action_update_sctips).setVisible(false);
    }


    public void check_for_updates(){
        if(NetworkUtils.isNetworkAvailable(progress.getContext())){
            p=0;
            //progress.setSmoothProgressDrawableColor(0);
            progress.progressiveStart();
            final int size=MangaService.getMap(MangaService.Type.All).values().size();
            //ProgressDrawable pr=new ProgressDrawable().setMax(size);
            if(getActivity()!=null){getActivity().invalidateOptionsMenu();}
            MangaService.isUpdating=true;
            for(Manga manga : MangaService.getMap(MangaService.Type.All).values()){
                if(!manga.isUpdated()){
                    manga.update(progress.getContext(), ()->{
                        MangaService.setCacheDirIfNull(manga.getSimilar());
                        adapterH.update(manga);
                        adapterS.update(manga);
                        for(Map.Entry<String,LimitedMangaAdapter> entry: adaptersF.entrySet()){
                            entry.getValue().update(manga);
                        }
                        if(manga.getNotCheckedNew()>0){progress.getContext().sendBroadcast(new Intent(Constants.action_Update_New).putExtra(Constants.hash,manga.hashCode()));}
                        if(++p==size){progress.progressiveStop(); MangaService.isUpdating=false; mainActivity.setNew(MangaService.getWithNew().size()); mainActivity.invalidateOptionsMenu();}
                        //pr.setProgress(p).setOnView(progress);
                    },throwable->{
                        if(++p==size){progress.progressiveStop(); MangaService.isUpdating=false; mainActivity.setNew(MangaService.getWithNew().size()); mainActivity.invalidateOptionsMenu();}
                        //pr.setProgress(p).setOnView(progress);
                    });
                }else{++p;}
            }
        }else{
            progress.progressiveStop();
            Toast.makeText(progress.getContext(), R.string.no_internet,Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        while(tasks.size()>0){
            tasks.removeFirst().run();
        }
        if(prefs.getBoolean(Constants.update_on_start,true) && !MangaService.isAllUpdated()){
            check_for_updates();
        }
        if(getActivity()!=null){getActivity().invalidateOptionsMenu();}
    }

    public HashSet<String> getShowingFavoriteCategories(SharedPreferences prefs){
        return MultiSelectListPreference.getEntries(prefs,Constants.categories,MangaService.defFavoriteCategory);
    }

    public LimitedMangaAdapter createAdapter(MangaService.Type type,String key){
        switch (type){
            default:
            case History:
                return adapterH=new LimitedMangaAdapter(MangaService.getSorted(MangaService.Type.History), MangaAdapter.Mode.MIXED, prefs.getInt(Constants.historyRows,4)>0 ? (prefs.getInt(Constants.historyRows,4)-1)*4+1 : 0,
                        manga -> startActivity(new Intent(getContext(), PreviewActivity.class).putExtra(Constants.hash,manga.hashCode())),
                        manga -> startActivity(new Intent(getContext(), ReaderActivity.class).putExtra(Constants.hash,manga.hashCode()).putExtra(Constants.history,true)
                        ));
            case Saved:
                return adapterS=new LimitedMangaAdapter(MangaService.getSorted(MangaService.Type.Saved),MangaAdapter.Mode.GRID,prefs.getInt(Constants.savedRows,4)*4,
                        manga -> startActivity(new Intent(getContext(), PreviewActivity.class).putExtra(Constants.hash,manga.hashCode())
                        ));
            case Favorites:
                adaptersF.put(key,new LimitedMangaAdapter(MangaService.getFavorites(key),MangaAdapter.Mode.GRID,prefs.getInt(Constants.favoritesRows,4)*3,
                        manga -> startActivity(new Intent(getContext(), PreviewActivity.class).putExtra(Constants.hash,manga.hashCode()))
                ));
                return adaptersF.get(key);
        }
    }

    public View createView(MangaAdapter adapter, GridLayoutManager layoutManager, String title, View.OnClickListener listener, LayoutInflater inflater, ViewGroup container){
        View root=inflater.inflate(R.layout.fragment_shelf, container, false);
        ((TextView)root.findViewById(R.id.title)).setText(title);
        root.findViewById(R.id.more).setOnClickListener(listener);
        adapter.initRV(root.findViewById(R.id.rv_list),layoutManager);
        views.put(adapter,root);
    return root;}

    public static boolean checkViewInChildren(ViewGroup viewGroup, View view){
        for(int i=0;i<viewGroup.getChildCount();i++){
            if(view==viewGroup.getChildAt(i)){return true;}
        }
        return false;
    }
    public void addViewIfAdapter(ViewGroup l,View view,LimitedMangaAdapter adapter, int index){
        if(adapter.getMaxCount()>0 && !checkViewInChildren(l,view)){l.addView(view,index);}
    }
    public void addViewIfAdapter(ViewGroup l,Hashtable<MangaAdapter, View> views,LimitedMangaAdapter adapter, int index){
        addViewIfAdapter(l,views.get(adapter),adapter,index);
    }
    public void removeViewIfAdapter(ViewGroup l,Hashtable<MangaAdapter, View> views,LimitedMangaAdapter adapter){
        if(adapter.getItemCount()==0){l.removeView(views.get(adapter));}
    }
}