package org.alex.kitsune.ui.shelf;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.manga.Manga_Scripted;
import org.alex.kitsune.scripts.Script;
import org.alex.kitsune.services.MangaService;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.HolderClickListener;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.ui.main.scripts.ScriptsActivity;
import org.alex.kitsune.ui.search.AdvancedSearchActivity;
import org.alex.kitsune.ui.settings.AuthorizationActivity;
import org.alex.kitsune.utils.LoadTask;
import org.alex.kitsune.utils.Updater;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Catalogs extends Fragment implements MenuProvider {
    RecyclerView rv;
    CatalogsAdapter adapter;
    ItemTouchHelper helper;
    SharedPreferences prefs;
    private static boolean updatingScrips=false;
    public static ArrayList<Container> containers=new ArrayList<>();
    FragmentActivity activity;

    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View root=inflater.inflate(R.layout.fragment_recyclerview_list,container,false);
        rv=root.findViewById(R.id.rv_list);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        prefs=PreferenceManager.getDefaultSharedPreferences(requireContext());
        adapter=new CatalogsAdapter(containers=getCatalogs(prefs), (v, index) -> requireContext().startActivity(new Intent(getContext(), AdvancedSearchActivity.class).putExtra(Constants.catalog,adapter.getSource(index))));
        rv.setAdapter(adapter);
        helper=new ItemTouchHelper(new ReorderCallback());
        helper.attachToRecyclerView(rv);
        activity=requireActivity();
        activity.removeMenuProvider(this);
        activity.addMenuProvider(this);
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
            case R.id.action_update_sctips:  Updater.checkAndUpdateScripts(activity, obj -> {updatingScrips=obj; activity.invalidateOptionsMenu();}); return true;
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
        if(dir!=null && dir.isDirectory() && (files=dir.listFiles(File::isFile))!=null){
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
        public String icon_url;
        public Container(Script script,Boolean enable,String cookies){
            this.script=script;
            this.source=script.getString(Constants.providerName,null);
            this.domain=script.getString(Constants.provider,null);
            this.enable=enable;
            this.cookies=cookies;
            this.icon_url=script.getString("icon","https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=96&url=http://"+domain);
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
        public static void loadSourceIcon(Container container, Callback<Boolean> callback){
            new LoadTask<Container,Void,Boolean>(){
                @Override
                protected Boolean doInBackground(Container container) {
                    return LoadTask.loadInBackground(container.icon_url,container.domain,new File(MangaService.getPathSourceIcon(container.domain)),null,null,false);
                }

                @Override
                protected void onFinished(Boolean downloaded) {
                    callback.call(downloaded);
                }

                @Override
                protected void onBraked(Throwable throwable) {
                    callback.call(false);
                }
            }.start(container);
        }
        public void getDrawableIconSource(Callback<Drawable> callback){
            Drawable drawable=Drawable.createFromPath(MangaService.getPathSourceIcon(domain));
            if(drawable==null){
                loadSourceIcon(this, obj -> callback.call(obj?Drawable.createFromPath(MangaService.getPathSourceIcon(domain)):null));
            }else{
                callback.call(drawable);
            }
        }
    }
    public static String getCookieByUrl(String url,String def){return Container.getCookieByUrl(containers,url,def);}
    public static String[] updateCookies(Context context, String source, String cookies){
        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(context);
        ArrayList<Container> containers=getCatalogs(prefs);
        for(Container c:containers){
            if(c.source.equals(source)){
                String[] original_decoded=filterCookies(source,cookies);
                c.cookies=original_decoded[0];
                prefs.edit().putString(Constants.source_order,Container.toJSON(containers).toString()).apply();
                return original_decoded;
            }
        }
        return new String[2];
    }
    public static String[] filterCookies(String source,String cookies_original){
        if(cookies_original==null){return new String[2];}
        String[] cookies=cookies_original.split("; ");
        String[] decoded=(android.os.Build.VERSION.SDK_INT >= 33? URLDecoder.decode(cookies_original, StandardCharsets.UTF_8) : URLDecoder.decode(cookies_original)).split("; ");
        StringBuilder text=new StringBuilder(),save=new StringBuilder();
        String[] tokens=Manga_Scripted.getScript(source).get("auth_tokens",String[].class);
        for(int i=0;i<cookies.length;i++){
            String[] nvd=decoded[i].split("=",2);
            try{decoded[i]=nvd[0]+"="+new JSONObject(nvd[1]).toString(2);}catch(JSONException ignored){}
            String tmp=nvd[0].toLowerCase();
            for(String token:tokens){
                if(tmp.contains(token.toLowerCase())){
                    text.append("\n\n").append(decoded[i]);
                    save.append("; ").append(cookies[i]);
                }
            }
        }
        return new String[]{save.length()==0?null:save.substring(2),text.length()==0?null:text.substring(2)};
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
                container.getDrawableIconSource(d->checkBox.setBackgroundDrawable(d));
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
