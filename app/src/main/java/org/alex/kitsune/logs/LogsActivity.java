package org.alex.kitsune.logs;


import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayoutMediator;
import org.alex.kitsune.R;
import org.alex.kitsune.utils.Utils;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LogsActivity extends AppCompatActivity{
    Toolbar toolbar;
    ViewPager2 pager;
    TextView log_text,log_date;
    LogsAdapter adapter;
    View more;
    Button cancel,send;
    MenuItem clearAllIcon,clearAllText;
    long lastDate=0;
    BottomSheetBehavior<View> behavior;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.Theme.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        toolbar.setTitle("Logs");
        if(Logs.getDir()==null){Logs.init(this);}
        adapter=new LogsAdapter(Logs.getLogs(Logs.getLogs()));
        pager=findViewById(R.id.pager);
        pager.setAdapter(adapter);
        new TabLayoutMediator(findViewById(R.id.tabs), pager, true, true, (tab, position) -> tab.setText(adapter.getTitle(position))).attach();
        more=findViewById(R.id.view_mode);
        cancel=findViewById(R.id.cancel);

        cancel.setOnClickListener(v -> showMore(false));
        send=more.findViewById(R.id.send);
        send.setOnClickListener(v -> Logs.sendLog(this,lastDate));
        log_text=more.findViewById(R.id.log_text);
        log_text.setHorizontallyScrolling(true);
        log_date=more.findViewById(R.id.log_date);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){@Override public void onPageSelected(int position){LogsActivity.this.onPageSelected(position);}});
        behavior=BottomSheetBehavior.from(more);
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull @NotNull View bottomSheet, int newState) {
                clearAllIcon.setVisible(!showingMore());
            }

            @Override
            public void onSlide(@NonNull @NotNull View bottomSheet, float slideOffset) {

            }
        });
    }
    private void onPageSelected(int position){if(clearAllIcon!=null && adapter.getItemCount()!=0){clearAllIcon.setVisible(adapter.getList(position).size()>0); clearAllText.setTitle(getString(R.string.clear_all)+" "+adapter.getTitle(position));}}
    private boolean showingMore(){return behavior.getState()!=BottomSheetBehavior.STATE_HIDDEN;}
    private void showMore(boolean show){
        if(show){
            behavior.setPeekHeight(more.getHeight(),true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }else{
            behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }
    public void ViewLog(Logs.Log log){
        log_text.setText(log.getSpannedString());
        log_date.setText(log.date());
        lastDate=log.date;
        showMore(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_logs, menu);
        clearAllIcon=menu.findItem(R.id.clear_all_icon);
        clearAllIcon.setVisible(adapter.getItemCount()!=0);
        clearAllText=menu.findItem(R.id.clear_all);
        onPageSelected(pager.getCurrentItem());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home: if(showingMore()){showMore(false);}else{finish();} break;
            case R.id.clear_all: adapter.clearAllIn(pager.getCurrentItem()); clearAllIcon.setVisible(adapter.getItemCount()!=0); if(pager.getCurrentItem()!=-1){onPageSelected(pager.getCurrentItem());} break;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            if(showingMore()){showMore(false);}else{finish();}
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    public class LogsListHolder extends RecyclerView.ViewHolder{
        RecyclerView rv;
        TextView nologs;
        Logs.LogsAdapter adapter;
        public LogsListHolder(@NonNull @NotNull ViewGroup parent, Logs.LogsAdapter.OnLogRemoveListener logRemoveListener) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_recyclerview_list,parent,false));
            rv=itemView.findViewById(R.id.rv_list);
            nologs=itemView.findViewById(R.id.text);
            nologs.setText(R.string.no_logs);
            rv.setLayoutManager(new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL));
            adapter=new Logs.LogsAdapter(new ArrayList<>(), (v,position) -> ViewLog(adapter.getLog(position)), count -> nologs.setVisibility(count==0 ? View.VISIBLE : View.GONE),logRemoveListener);
            rv.setAdapter(adapter);
            Utils.registerOnEmptyAdapterRunnable(adapter,()->nologs.setVisibility(adapter.getItemCount()==0?View.VISIBLE:View.GONE));
        }
        public void bind(List<Logs.Log> logs){
            adapter.setLogs(logs);
        }
    }
    public class LogsAdapter extends RecyclerView.Adapter<LogsListHolder>{
        HashMap<String, ArrayList<Logs.Log>> map;
        public LogsAdapter(HashMap<String, ArrayList<Logs.Log>> map){
            this.map=map;
        }
        @NonNull
        @NotNull
        @Override
        public LogsListHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            return new LogsListHolder(parent,log -> {String key=log.getType(); ArrayList<Logs.Log> logs=map.get(key); if(logs!=null){logs.remove(log);} if(logs==null || logs.size()==0){map.remove(key); notifyItemRemoved(pager.getCurrentItem()); onPageSelected(pager.getCurrentItem());}});
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull LogsListHolder holder, int position) {
            holder.bind(getList(position));
        }

        public String getTitle(int position){return map.keySet().toArray(new String[0])[position];}
        public ArrayList getList(int position){return map.values().toArray(new ArrayList[0])[position];}
        public void clearAllIn(int position){
            String key=map.keySet().toArray(new String[0])[position];
            ArrayList<Logs.Log> logs=map.remove(key);
            if(logs!=null){for(Logs.Log log:logs){Logs.clearLog(log.date);}}
            notifyItemRemoved(position);
        }
        @Override
        public int getItemCount(){return map.size();}
    }
}