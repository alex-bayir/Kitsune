package org.alex.kitsune.ui.search;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayoutMediator;
import org.alex.kitsune.R;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.ui.shelf.Catalogs;
import org.alex.kitsune.utils.Utils;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class RecommendationsActivity extends AppCompatActivity {
    Toolbar toolbar;
    ViewPager2 pager;
    Adapter adapter;

    int[] stringIds={R.string.sort_popular,R.string.sort_latest,R.string.sort_updated};
    ArrayList<Catalogs.Container> catalogs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.Theme.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recomendations);
        catalogs=Catalogs.getCatalogs(PreferenceManager.getDefaultSharedPreferences(this));
        pager=findViewById(R.id.pager);
        adapter=new Adapter(stringIds);
        pager.setAdapter(adapter);
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setTitle(R.string.recommendations);
        new TabLayoutMediator(findViewById(R.id.tabs), pager, true, true, (tab, position) -> tab.setText(stringIds[position])).attach();
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                adapter.update(intent.getIntExtra(Constants.hash,-1));
            }
        },new IntentFilter(Constants.action_Update));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home -> finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public class Adapter extends RecyclerView.Adapter<Adapter.Holder>{
        int[] orders;
        SourceSearchAdapter[] adapters;
        public Adapter(int[] orders){
            this.orders=orders;
            adapters=new SourceSearchAdapter[orders.length];
        }
        public void update(int hash){
            for(SourceSearchAdapter adapter:adapters){if(adapter!=null){adapter.update(hash);}}
        }
        @Override
        public int getItemViewType(int position){return position;}

        @NonNull
        @NotNull
        @Override
        public Holder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            return new Holder(parent,viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull Holder holder, int position) {
            adapters[position]=holder.getAdapter();
        }

        @Override public int getItemCount(){return orders.length;}

        public class Holder extends RecyclerView.ViewHolder{
            SourceSearchAdapter adapter;
            RecyclerView rv;
            TextView error;
            public Holder(@NonNull @NotNull ViewGroup parent,int order) {
                super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_recyclerview_list,parent,false));
                error=itemView.findViewById(R.id.text);
                rv=itemView.findViewById(R.id.rv_list);
                rv.setLayoutManager(new LinearLayoutManager(rv.getContext(),RecyclerView.VERTICAL,false));
                adapter=new SourceSearchAdapter(catalogs.stream().filter(c->c.enable).map(c->c.source).collect(Collectors.toList()),true);
                rv.setAdapter(adapter);
                rv.setVerticalScrollBarEnabled(false);
                SearchActivity.search(rv.getContext(),null,order,adapter.getSources(),adapter.getCallback(),error);
            }

            public SourceSearchAdapter getAdapter(){return adapter;}
        }

    }
}