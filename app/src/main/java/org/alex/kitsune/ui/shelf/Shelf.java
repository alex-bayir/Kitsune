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
import org.alex.kitsune.ui.preview.PreviewActivity;
import org.alex.kitsune.ui.reader.ReaderActivity;
import org.alex.kitsune.ui.settings.SettingsShelf;
import org.alex.kitsune.ui.shelf.favorite.FavoritesActivity;
import org.alex.kitsune.ui.shelf.history.HistoryActivity;
import org.alex.kitsune.ui.shelf.saved.SavedActivity;
import org.alex.kitsune.utils.NetworkUtils;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.function.BiConsumer;


public class Shelf extends Fragment implements MenuProvider {
    LinearLayout l;
    Hashtable<String,LimitedMangaAdapter> adapters;
    Hashtable<MangaAdapter,View> views;
    public static final String History=Constants.history,Saved=Constants.saved;
    LinkedHashMap<String,Integer> sequence=new LinkedHashMap<>();
    LinkedHashMap<String, SettingsShelf.Container> shelf_sequence=new LinkedHashMap<>();
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
                                manageViewIfAdapter(l,views,adapters.get(History),0).add(0,manga);
                                for(Map.Entry<String,LimitedMangaAdapter> entry: adapters.entrySet()){
                                    entry.getValue().update(manga);
                                }
                                break;
                            case Constants.load:
                                manageViewIfAdapter(l,views,adapters.get(Saved),-1).add(0,manga);
                                for(Map.Entry<String,LimitedMangaAdapter> entry: adapters.entrySet()){
                                    entry.getValue().update(manga);
                                }
                                break;
                            case Constants.favorites:
                                for(Map.Entry<String,LimitedMangaAdapter> entry: adapters.entrySet()){
                                    String key=entry.getKey();
                                    if(History.equals(key) || Saved.equals(key)){
                                        entry.getValue().update(manga);
                                    }else if(key.equals(manga.getCategoryFavorite())){
                                        entry.getValue().add(0,manga);
                                    }else{
                                        entry.getValue().remove(manga);
                                    }
                                    manageViewIfAdapter(l,views,entry.getValue(),getOrDefault(sequence,key,-1));
                                }
                                String key=manga.getCategoryFavorite();
                                if(key!=null && !adapters.containsKey(key)){
                                    createView(createAdapter(MangaService.Type.Favorites,key),new GridLayoutManager(getContext(),3),key, v -> startActivity(new Intent(getContext(), FavoritesActivity.class).putExtra(Constants.category,key)),LayoutInflater.from(getContext()),l);
                                    manageViewIfAdapter(l,views, adapters.get(key),getOrDefault(sequence,key,-1));
                                }
                                break;
                            default:
                                if(manga.getHistory()!=null){
                                    adapters.get(History).update(manga);
                                }else{
                                    manageViewIfAdapter(l,views,adapters.get(History),-1).remove(manga);
                                }
                                if(manga.countSaved()>0){
                                    adapters.get(Saved).update(manga);
                                }else{
                                    manageViewIfAdapter(l,views,adapters.get(Saved),-1).remove(manga);
                                }
                                for(Map.Entry<String,LimitedMangaAdapter> entry: adapters.entrySet()){
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

    private <K,V> V getOrDefault(HashMap<K,V> map,K key,V def){
        V value=map.get(key);
        return value!=null?value:def;
    }
    private void update(LayoutInflater inflater){
        l.removeAllViews();
        views=new Hashtable<>();
        adapters=new Hashtable<>();
        sequence=new LinkedHashMap<>();
        shelf_sequence=SettingsShelf.getShelfSequence(prefs);
        int i=0;
        for(Map.Entry<String,SettingsShelf.Container> entry:shelf_sequence.entrySet()){
            String key=entry.getKey();
            SettingsShelf.Container value=entry.getValue();
            sequence.put(key,value.count>0? i++ : -1);
            switch (key){
                case History->{
                    createView(createAdapter(MangaService.Type.History,key),MangaAdapter.create(getContext(), 4, value.first?position->position==0 ? 4:1:null),getString(R.string.History), v -> startActivity(new Intent(getContext(), HistoryActivity.class)),inflater, l);
                    manageViewIfAdapter(l,views,adapters.get(key),-1);
                }
                case Saved->{
                    createView(createAdapter(MangaService.Type.Saved,key),MangaAdapter.create(getContext(),4, value.first?position->position==0 ? 4:1:null),getString(R.string.Saved), v -> startActivity(new Intent(getContext(), SavedActivity.class)),inflater, l);
                    manageViewIfAdapter(l,views,adapters.get(key),-1);
                }
                default -> {
                    createView(createAdapter(MangaService.Type.Favorites,key),MangaAdapter.create(getContext(),3, value.first?position->position==0 ? 3:1:null),key, v -> startActivity(new Intent(getContext(), FavoritesActivity.class).putExtra(Constants.category,key)),inflater,l);
                    manageViewIfAdapter(l,views, adapters.get(key),-1);
                }
            }
        }
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
                        for(Map.Entry<String,LimitedMangaAdapter> entry: adapters.entrySet()){
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

    public LimitedMangaAdapter createAdapter(MangaService.Type type,String key){
        switch (type){
            default:
            case History:
                adapters.put(History,new LimitedMangaAdapter(MangaService.getSorted(MangaService.Type.History), MangaAdapter.Mode.MIXED, getOrDefault(shelf_sequence,key,new SettingsShelf.Container(key,4,false)).count,
                        manga -> startActivity(new Intent(getContext(), PreviewActivity.class).putExtra(Constants.hash,manga.hashCode())),
                        manga -> startActivity(new Intent(getContext(), ReaderActivity.class).putExtra(Constants.hash,manga.hashCode()).putExtra(Constants.history,true)
                        )));
                return adapters.get(History);
            case Saved:
                adapters.put(Saved,new LimitedMangaAdapter(MangaService.getSorted(MangaService.Type.Saved),MangaAdapter.Mode.GRID,getOrDefault(shelf_sequence,key,new SettingsShelf.Container(key,4,false)).count,
                        manga -> startActivity(new Intent(getContext(), PreviewActivity.class).putExtra(Constants.hash,manga.hashCode())
                        )));
                return adapters.get(Saved);
            case Favorites:
                adapters.put(key,new LimitedMangaAdapter(MangaService.getFavorites(key),MangaAdapter.Mode.GRID,getOrDefault(shelf_sequence,key,new SettingsShelf.Container(key,4,false)).count,
                        manga -> startActivity(new Intent(getContext(), PreviewActivity.class).putExtra(Constants.hash,manga.hashCode()))
                ));
                return adapters.get(key);
        }
    }

    public View createView(LimitedMangaAdapter adapter, GridLayoutManager layoutManager, String title, View.OnClickListener listener, LayoutInflater inflater, ViewGroup container){
        View root=inflater.inflate(R.layout.fragment_shelf, container, false);
        ((TextView)root.findViewById(R.id.title)).setText(title);
        root.findViewById(R.id.more).setOnClickListener(listener);
        adapter.initRV(root.findViewById(R.id.rv_list),layoutManager, adapter.getMaxCount());
        views.put(adapter,root);
    return root;}

    public static boolean checkViewInChildren(ViewGroup viewGroup, View view){
        for(int i=0;i<viewGroup.getChildCount();i++){
            if(view==viewGroup.getChildAt(i)){return true;}
        }
        return false;
    }
    public LimitedMangaAdapter manageViewIfAdapter(ViewGroup l,Hashtable<MangaAdapter, View> views,LimitedMangaAdapter adapter, int index){
        return manageViewIfAdapter(l,views.get(adapter),adapter,index);
    }
    public LimitedMangaAdapter manageViewIfAdapter(ViewGroup l,View view,LimitedMangaAdapter adapter, int index){
        if(adapter.getItemCount()==0){
            l.removeView(view);
        }else if(!checkViewInChildren(l,view)){
            l.addView(view,index);
        }
        return adapter;
    }
}