package org.alex.kitsune.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.HolderClickListener;
import org.alex.kitsune.services.MangaService;
import org.alex.kitsune.ui.main.Constants;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;

public class SettingsShelf  extends Fragment {
    RecyclerView rv;
    ItemTouchHelper helper;
    Adapter adapter;
    ArrayList<Container> containers=new ArrayList<>();
    SharedPreferences prefs;
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_recyclerview_list,container,false);
        prefs=PreferenceManager.getDefaultSharedPreferences(requireContext());
        rv=view.findViewById(R.id.rv_list);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter=new Adapter(containers=getSequence(prefs));
        rv.setAdapter(adapter);
        helper=new ItemTouchHelper(new ReorderCallback());
        helper.attachToRecyclerView(rv);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        save(prefs,adapter.getList());
    }

    @Override
    public void onResume() {
        super.onResume();
        if(adapter!=null){adapter.update(containers=getSequence(prefs));}
    }

    public static void save(SharedPreferences prefs, Collection<Container> list){prefs.edit().putString(Constants.shelf_order,Container.toJSON(list).toString()).apply();}
    public static ArrayList<Container> getSequence(SharedPreferences prefs){return new ArrayList<>(getShelfSequence(prefs).values());}
    public static LinkedHashMap<String, Container> getShelfSequence(SharedPreferences prefs){
        LinkedHashMap<String, Container> map=new LinkedHashMap<>();
        for(Container container: Container.fromJSON(prefs.getString(Constants.shelf_order, ""))){
            if(container!=null && container.name!=null){map.put(container.name, container);}
        }
        Set<String> set=new HashSet<>(MangaService.getCategories());
        map.putIfAbsent(Constants.history, new Container(Constants.history,4,true));
        for(String category:set){
            map.putIfAbsent(category, new Container(category,4,false));
        }
        map.putIfAbsent(Constants.saved, new Container(Constants.saved,4,false));
        set.add(Constants.history);
        set.add(Constants.saved);
        ArrayList<Container> containers=new ArrayList<>(map.values());
        for(Container container:containers){
            if(!set.contains(container.name)){map.remove(container.name);}
        }
        return map;
    }
    public static void add(SharedPreferences prefs,String category){
        LinkedHashMap<String, Container> map=getShelfSequence(prefs);
        map.putIfAbsent(category,new Container(category,4,false));
        save(prefs,map.values());
    }
    public static class Container{
        public String name;
        public int count;
        public boolean first;
        public Container(String name,int count,boolean first){
            this.name=name;
            this.count=count;
            this.first=first;
        }
        public JSONObject toJSON(){
            try{return new JSONObject().put("name",name).put("count",count).put("first", first);}catch(JSONException e){return null;}
        }
        public static Container fromJSON(JSONObject json){
            try{
                return new Container(json.optString("name"), json.optInt("count"),json.optBoolean("first", false));
            }catch (Throwable e){
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
    }
    public class Adapter extends RecyclerView.Adapter<Holder>{
        ArrayList<Container> list;
        public Adapter(ArrayList<Container> list){this.list=list;}
        public ArrayList<Container> getList(){return list;}
        public void update(ArrayList<Container> list){
            this.list=list;
            notifyDataSetChanged();
        }
        @NonNull
        @NotNull
        @Override
        public Holder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            return new Holder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull Holder holder, int position) {
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
    }
    public class Holder extends RecyclerView.ViewHolder{
        CheckBox checkBox;
        TextView name,seekbar_value;
        ImageView reorder;
        SeekBar seekBar;
        Container container;

        public Holder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shelf_pref,parent,false));
            checkBox=itemView.findViewById(R.id.checkbox);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked)->{if(container!=null){container.first =isChecked;}});
            name=itemView.findViewById(R.id.source);
            seekBar=itemView.findViewById(R.id.seekBar);
            seekbar_value=itemView.findViewById(R.id.seekbar_value);
            reorder=itemView.findViewById(R.id.reorder);
            reorder.setOnTouchListener((v, event) -> {
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN: helper.startDrag(Holder.this); break;
                }
                return false;
            });
            seekBar.setMax(5);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                    seekbar_value.setText(String.valueOf(progress));
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar){}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    container.count=seekBar.getProgress();
                }
            });
        }
        public void bind(Container container){
            this.container=container;
            name.setText(container.name);
            checkBox.setChecked(container.first);
            seekBar.setProgress(container.count);
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
            if(recyclerView.getAdapter() instanceof Adapter){
                ((Adapter)recyclerView.getAdapter()).moved(pos1,pos2);
            }
            return true;
        }

        @Override
        public void onSwiped(@NonNull @NotNull RecyclerView.ViewHolder viewHolder, int direction) {}
    }
}
