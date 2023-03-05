package org.alex.kitsune.ui.search;

import android.content.Intent;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayoutMediator;
import fr.castorflex.android.circularprogressbar.CircularProgressBar;
import fr.castorflex.android.circularprogressbar.CircularProgressDrawable;
import org.alex.kitsune.R;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.manga.views.MangaAdapter;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.services.MangaService;
import org.alex.kitsune.ui.preview.PreviewActivity;
import org.alex.kitsune.ui.shelf.Catalogs;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Utils;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RecommendationsActivity extends AppCompatActivity {
    Toolbar toolbar;
    ViewPager2 pager;
    Adapter adapter;
    Manga updateOnReturn=null;

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.updateManga(updateOnReturn);
        updateOnReturn=null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_search, menu);
        menu.findItem(adapter.isShowSource() ? R.id.source : R.id.status).setChecked(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home -> finish();
            case (R.id.list) -> {item.setChecked(true);adapter.setSpanCount(1);}
            case (R.id.largeGrid) -> {item.setChecked(true);adapter.setSpanCount(2);}
            case (R.id.mediumGrid) -> {item.setChecked(true);adapter.setSpanCount(3);}
            case (R.id.smallGrid) -> {item.setChecked(true);adapter.setSpanCount(4);}
            case (R.id.status), (R.id.source) -> {item.setChecked(true);adapter.setShowSource(item.getItemId() == R.id.source);}
        }
        return super.onOptionsItemSelected(item);
    }

    public class Adapter extends RecyclerView.Adapter<Adapter.Holder>{
        boolean showSource=true;
        int spanCount=1;
        int[] orders;
        ArrayList<MangaAdapter> adapters=new ArrayList<>();
        public Adapter(int[] orders){this.orders=orders;}
        public void setShowSource(boolean showSource){this.showSource=showSource; for(MangaAdapter adapter:adapters){adapter.setShowSource(this.showSource);}}
        public boolean isShowSource(){return showSource;}
        public void setSpanCount(int spanCount){this.spanCount=spanCount; for(MangaAdapter adapter:adapters){adapter.setSpanCount(spanCount);}}

        @Override
        public int getItemViewType(int position){return position+1;}

        @NonNull
        @NotNull
        @Override
        public Holder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            Holder holder=new Holder(parent,viewType);
            adapters.add(holder.getAdapter());
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull Holder holder, int position) {}

        @Override public int getItemCount(){return orders.length;}

        public void updateManga(Manga manga){for(MangaAdapter adapter:adapters){adapter.update(manga);}}

        public class Holder extends RecyclerView.ViewHolder{
            MangaAdapter adapter;
            TextView nothingFound;
            CircularProgressBar progressBar;
            private int loaded=0;
            public Holder(@NonNull @NotNull ViewGroup parent,int order) {
                super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_recyclerview_list_with_progerss,parent,false));
                nothingFound=itemView.findViewById(R.id.text);
                progressBar=itemView.findViewById(R.id.progress);
                progressBar.setIndeterminateDrawable(new CircularProgressDrawable.Builder(itemView.getContext()).colors(new int[]{0xff0000ff,0xff00ffff,0xff00ff00,0xffffff00,0xffff0000,0xffff00ff}).style(CircularProgressDrawable.STYLE_ROUNDED).strokeWidth(8f).sweepInterpolator(new AccelerateDecelerateInterpolator()).build());

                adapter=new MangaAdapter(null, MangaAdapter.Mode.LIST, manga -> {
                    adapter.add(updateOnReturn=MangaService.getOrPutNewWithDir(manga));
                    parent.getContext().startActivity(new Intent(parent.getContext(), PreviewActivity.class).putExtra(Constants.hash,manga.hashCode()));
                });
                adapter.setShowSource(showSource);
                adapter.initRV(itemView.findViewById(R.id.rv_list),spanCount);
                if(NetworkUtils.isNetworkAvailable(itemView.getContext())){
                    progressBar.setVisibility(View.VISIBLE); int i=0;
                    for(Catalogs.Container container:catalogs){if(container.enable){i++; SearchActivity.searchManga(container.source, null, order, this::add, nothingFound);}}
                    if(i==0){
                        nothingFound.setText(R.string.no_catalogs_selected);
                        nothingFound.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    }
                }else{
                    nothingFound.setText(R.string.internet_is_loss);
                    nothingFound.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }
            }

            public MangaAdapter getAdapter(){return adapter;}

            private synchronized void add(Collection<Manga> mangas){
                if(mangas!=null && mangas.size()>0){
                    mangas=mangas instanceof List ? mangas : new ArrayList<>(mangas);
                    MangaService.setCacheDirIfNull((List<Manga>)mangas);
                    adapter.addAll(mangas,Manga.SourceComparator(Catalogs.Container.sources(catalogs)));
                    progressBar.setVisibility(View.GONE);
                }
                if(++loaded==Catalogs.Container.countEnabled(catalogs)){
                    progressBar.setVisibility(View.GONE);
                    if(adapter.getItemCount()==0){nothingFound.setVisibility(View.VISIBLE);}
                }

            }
        }

    }
}