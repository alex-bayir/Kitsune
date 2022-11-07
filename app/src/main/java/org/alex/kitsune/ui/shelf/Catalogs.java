package org.alex.kitsune.ui.shelf;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.manga.Manga_Scripted;
import org.alex.kitsune.scripts.Script;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.HolderClickListener;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.ui.main.scripts.ScriptsActivity;
import org.alex.kitsune.ui.search.AdvancedSearchActivity;
import org.alex.kitsune.ui.settings.AuthorizationActivity;
import org.alex.kitsune.utils.Updater;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Catalogs extends Fragment implements MenuProvider {
    RecyclerView rv;
    CatalogsAdapter adapter;
    ItemTouchHelper helper;
    SharedPreferences prefs;
    private static boolean updatingScrips=false;
    public static ArrayList<Container> containers=new ArrayList<>();

    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View root=inflater.inflate(R.layout.fragment_recyclerview_list,container,false);
        rv=root.findViewById(R.id.rv_list);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        prefs=PreferenceManager.getDefaultSharedPreferences(getContext());
        adapter=new CatalogsAdapter(containers=getCatalogs(prefs), (v, index) -> getContext().startActivity(new Intent(getContext(), AdvancedSearchActivity.class).putExtra(Constants.catalog,adapter.getSource(index))));
        rv.setAdapter(adapter);
        helper=new ItemTouchHelper(new ReorderCallback());
        helper.attachToRecyclerView(rv);
        requireActivity().removeMenuProvider(this);
        requireActivity().addMenuProvider(this);
        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        save(prefs);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getActivity()!=null){getActivity().invalidateOptionsMenu();}
        if(adapter!=null){adapter.update(containers=getCatalogs(prefs));}
    }


    @Override
    public void onCreateMenu(@NonNull @NotNull Menu menu, @NonNull @NotNull MenuInflater menuInflater){}

    @Override
    public void onPrepareMenu(@NonNull @NotNull Menu menu) {
        menu.findItem(R.id.action_find_manga).setVisible(true);
        menu.findItem(R.id.check_for_updates).setVisible(false);
        menu.findItem(R.id.action_add_source).setVisible(true);
        menu.findItem(R.id.full).setVisible(false);
        menu.findItem(R.id.action_update_sctips).setVisible(true).setEnabled(!updatingScrips);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull @NotNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_add_source: startActivity(new Intent(getContext(), ScriptsActivity.class)); return true;
            case R.id.action_update_sctips:  Updater.checkAndUpdateScripts(getActivity(), obj -> {updatingScrips=obj; requireActivity().invalidateOptionsMenu();}); return true;
        }
        return false;
    }

    public void save(SharedPreferences prefs){prefs.edit().putString(Constants.source_order,adapter.getSave()).apply();}

    public static ArrayList<Container> getCatalogs(SharedPreferences prefs){
        LinkedHashMap<String,Container> map=new LinkedHashMap<>();
        Hashtable<String,Script> scripts=Manga_Scripted.getScripts();
        boolean exist=prefs.contains(Constants.source_order);
        if(exist){
            for(Container container:Container.fromJSON(prefs.getString(Constants.source_order, ""))){
                if(container!=null && container.source!=null){map.put(container.source, container);}
            }
        }
        for(Map.Entry<String,Script> entry:scripts.entrySet()){
            map.putIfAbsent(entry.getKey(), new Container(entry.getValue(),true,null));
        }
        return new ArrayList<>(exist?map.values():map.values().stream().sorted(Comparator.comparingInt(o -> default_order.indexOf(o.source))).collect(Collectors.toList()));
    }
    public static List<String> default_order=Arrays.asList("Desu","MangaLib","Remanga","ReadManga","MintManga","SelfManga","MangaChan","HentaiChan");
    public static ArrayList<Script> getScripts(File dir, boolean recur){
        File[] files=dir.listFiles();
        if(files==null){return new ArrayList<>();}
        ArrayList<Script> scripts=new ArrayList<>(files.length);
        for(File file:files){
            if(file.isDirectory() && recur){
                scripts.addAll(getScripts(file,true));
            }else if(file.isFile()){
                try{scripts.add(Script.getInstance(file));}catch (Throwable e){e.printStackTrace();}
            }
        }
        return scripts;
    }
    public static Hashtable<String,Script> getMangaScripts(File dir){
        Hashtable<String,Script> table=new Hashtable<>(); File[] files;
        if(dir!=null && dir.isDirectory() && (files=dir.listFiles())!=null){
            for(File file:files){
                try{
                    Script script=Script.getInstance(file); String source=script.get(Constants.providerName,String.class);
                    if(source!=null){table.put(source,script);}
                }catch (Throwable e){
                    e.printStackTrace();
                }
            }
        }
        return table;
    }

    public static class Container{
        public String source;
        public String domain;
        public Boolean enable;
        public Script script;
        public String cookies;
        public Container(Script script,Boolean enable,String cookies){
            this.script=script;
            this.source=script.getString(Constants.providerName,null);
            this.domain=script.getString(Constants.provider,null);
            this.enable=enable;
            this.cookies=cookies;
        }
        public JSONObject toJSON(){
            try{return script==null ? null : new JSONObject().put("source",source).put("enable",enable).put("script",script.getPath()).put("cookies",cookies);}catch(JSONException e){return null;}
        }
        public static Container fromJSON(JSONObject json){
            try{
                return new Container(Manga_Scripted.getScript(json.optString("source"), json.optString("script")),json.optBoolean("enable", true),json.optString("cookies",null));
            }catch (JSONException e){
                return null;
            }catch (Throwable e){
                Logs.saveLog(e);
                return null;
            }
        }
        public static JSONArray toJSON(Collection<Container> list){
            JSONArray jsonArray=new JSONArray();
            for(Container container:list){
                if(container.toJSON()!=null){
                    jsonArray.put(container.toJSON());
                }
            }
            return jsonArray;
        }
        public static ArrayList<Container> fromJSON(String json){
            ArrayList<Container> list=new ArrayList<>(10);
            try {
                JSONArray jsonArray=new JSONArray(json);
                for(int i=0;i<jsonArray.length();i++){
                    list.add(Container.fromJSON(jsonArray.getJSONObject(i)));
                }
            }catch (JSONException e){e.printStackTrace();}
            return list;
        }
        public static int countEnabled(List<Container> containers){
            int enabled=0;for(Container container:containers){if(container.enable){enabled++;}}return enabled;
        }
        public static ArrayList<String> sources(List<Container> containers){
            ArrayList<String> list=new ArrayList<>(containers.size());
            for(Container container: containers){list.add(container.source);}
            return list;
        }
        public static String getCookies(Container container, String def){
            return container!=null ? container.cookies : def;
        }
        public static Container getContainerByUrl(Collection<Container> containers,String url){
            return url!=null?containers.stream().filter(c->url.contains(c.domain)).findFirst().orElse(null):null;
        }
        public static String getCookieByUrl(Collection<Container> containers,String url,String def){
            return url!=null?getCookies(getContainerByUrl(containers,url), def):def;
        }
    }
    public static String getCookieByUrl(String url,String def){return Container.getCookieByUrl(containers,url,def);}
    public static String[] updateCookies(Context context, String source, String cookies){
        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(context);
        ArrayList<Container> containers=getCatalogs(prefs);
        for(Container c:containers){
            if(c.source.equals(source)){
                String[] original_decoded=Manga_Scripted.filterCookies(source,cookies);
                c.cookies=original_decoded[0];
                prefs.edit().putString(Constants.source_order,Container.toJSON(containers).toString()).apply();
                return original_decoded;
            }
        }
        return new String[2];
    }

    public class CatalogsAdapter extends RecyclerView.Adapter<CatalogsAdapter.CatalogHolder>{
        ArrayList<Container> list;
        final HolderClickListener listener;
        public CatalogsAdapter(ArrayList<Container> list, HolderClickListener listener){this.list=list; this.listener=listener;}
        public String getSave(){return Container.toJSON(list).toString();}
        public void update(ArrayList<Container> list){
            this.list=list;
            notifyDataSetChanged();
        }
        @NonNull
        @NotNull
        @Override
        public CatalogsAdapter.CatalogHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            return new CatalogsAdapter.CatalogHolder(parent,listener);
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull CatalogsAdapter.CatalogHolder holder, int position) {
            holder.bind(list.get(position));
        }

        @Override
        public int getItemCount(){return list.size();}

        public void moved(int pos1,int pos2){
            Container tmp=list.get(pos1);
            list.set(pos1,list.get(pos2));
            list.set(pos2,tmp);
            notifyItemMoved(pos1, pos2);
        }
        public String getSource(int pos){return list.get(pos).source;}
        public class CatalogHolder extends RecyclerView.ViewHolder{
            AppCompatCheckBox checkBox;
            TextView name,description;
            ImageView reorder,login;
            Container container;

            public CatalogHolder(ViewGroup parent, HolderClickListener listener) {
                super(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_catalog,parent,false));
                itemView.setOnClickListener(view->listener.onItemClick(view,getBindingAdapterPosition()));
                checkBox=itemView.findViewById(R.id.checkbox);
                checkBox.setOnCheckedChangeListener((buttonView, isChecked)->{if(container!=null){container.enable=isChecked;}});
                name=itemView.findViewById(R.id.source);
                description=itemView.findViewById(R.id.description);
                reorder=itemView.findViewById(R.id.reorder);
                login=itemView.findViewById(R.id.login);
                reorder.setOnTouchListener((v, event) -> {
                    switch(event.getAction()){
                        case MotionEvent.ACTION_DOWN: helper.startDrag(CatalogsAdapter.CatalogHolder.this); break;
                    }
                    return false;
                });
                login.setOnClickListener(v -> {
                    if(container!=null && container.source!=null){
                        v.getContext().startActivity(new Intent(v.getContext(), AuthorizationActivity.class).putExtra("source",container.source));
                    }
                });
            }
            public void bind(Container catalog){
                container=catalog;
                name.setText(catalog.source);
                checkBox.setChecked(catalog.enable);
                description.setText(Manga.getSourceDescription(catalog.source));
                login.getDrawable().setTint(container.cookies!=null? Color.GREEN:Color.RED);
            }
        }
    }

    public static class ReorderCallback extends ItemTouchHelper.Callback{
        private final int MOVEMENT_FLAGS=makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);

        public ReorderCallback(){}

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }
        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }
        @Override
        public int getMovementFlags(@NonNull @NotNull RecyclerView recyclerView, @NonNull @NotNull RecyclerView.ViewHolder viewHolder) {
            return MOVEMENT_FLAGS;
        }

        @Override
        public boolean onMove(@NonNull @NotNull RecyclerView recyclerView, @NonNull @NotNull RecyclerView.ViewHolder viewHolder, @NonNull @NotNull RecyclerView.ViewHolder target) {
            int pos1 = viewHolder.getBindingAdapterPosition();
            int pos2 = target.getBindingAdapterPosition();
            if(recyclerView.getAdapter() instanceof CatalogsAdapter){
                ((CatalogsAdapter)recyclerView.getAdapter()).moved(pos1,pos2);
            }
            return true;
        }

        @Override
        public void onSwiped(@NonNull @NotNull RecyclerView.ViewHolder viewHolder, int direction) {}
    }
}
