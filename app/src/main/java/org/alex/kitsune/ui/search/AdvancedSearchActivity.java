package org.alex.kitsune.ui.search;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import fr.castorflex.android.circularprogressbar.CircularProgressBar;
import fr.castorflex.android.circularprogressbar.CircularProgressDrawable;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.manga.Manga_Scripted;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.services.MangaService;
import org.alex.kitsune.R;
import org.alex.kitsune.manga.search.FilterSortAdapter;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.manga.views.MangaAdapter;
import org.alex.kitsune.ui.preview.PreviewActivity;
import org.alex.kitsune.utils.LoadTask;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Utils;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;


public class AdvancedSearchActivity extends AppCompatActivity implements Callback<Object> {
    Toolbar toolbar;
    FilterDialogFragment filters;
    RecyclerView rv;
    MangaAdapter adapter;
    Manga updateOnReturn=null;
    FilterSortAdapter sortAdapter;
    String queryName=null;
    String source;
    TextView nothingFound;
    private boolean enableLoadMore=false;
    private int page=0;
    CircularProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.Theme.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_search);
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        progressBar=findViewById(R.id.progress);
        progressBar.setIndeterminateDrawable(new CircularProgressDrawable.Builder(this).colors(new int[]{0xff0000ff,0xff00ffff,0xff00ff00,0xffffff00,0xffff0000,0xffff00ff}).style(CircularProgressDrawable.STYLE_ROUNDED).strokeWidth(8f).sweepInterpolator(new AccelerateDecelerateInterpolator()).build());
        progressBar.setVisibility(View.INVISIBLE);

        nothingFound=findViewById(R.id.text);
        nothingFound.setText(R.string.nothing_found);
        source=getIntent().getStringExtra(Constants.catalog);
        toolbar.setTitle(source);
        sortAdapter=Manga.getFilterSortAdapter(source);
        if(getIntent().getStringExtra(Constants.author)!=null){
            toolbar.setTitle(getIntent().getStringExtra(Constants.author));
        }
        if(sortAdapter!=null){
            sortAdapter.selectOption(getIntent().getStringExtra(Constants.option));
            queryName=getIntent().getStringExtra(Constants.author_url);
            if(queryName!=null && queryName.length()==0){queryName=null;}
        }
        filters=new FilterDialogFragment(sortAdapter,this);
        View fab=findViewById(R.id.fab);
        if(sortAdapter==null || sortAdapter.getOptions()==null || sortAdapter.getOptions().length==0){nothingFound.setText(R.string.no_advanced_search_adapter); nothingFound.setVisibility(View.VISIBLE); fab.setVisibility(View.INVISIBLE);}
        fab.setOnClickListener(view -> {
            view.animate().scaleY(0.75f).scaleX(0.75f).setDuration(200).setInterpolator(new LinearInterpolator()).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.animate().scaleY(1).scaleX(1).setDuration(200).setInterpolator(new LinearInterpolator()).setListener(null).start();
                }
            }).start();
            filters.show(getSupportFragmentManager(), "");
        });
        rv=findViewById(R.id.rv_list);
        adapter=new MangaAdapter(null, MangaAdapter.Mode.LIST, manga -> {
            adapter.add(updateOnReturn=MangaService.getOrPutNewWithDir(manga));
            startActivity(new Intent(this, PreviewActivity.class).putExtra(Constants.hash,manga.hashCode()));
        });
        adapter.initRV(rv,1);
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull @NotNull RecyclerView recyclerView, int dx, int dy) {
                if(enableLoadMore && findLastVisibleItemPosition(rv)>getItemCount(rv)-3){load(queryName);}
            }
        });
        call(queryName);
    }



    @Override
    public void call(Object obj){
        adapter.clear();
        page=0;
        load(obj instanceof String ? (String) obj : null);
    }
    public void load(String query){
        if(NetworkUtils.isNetworkAvailable(this)){
            enableLoadMore=false;
            nothingFound.setVisibility(View.GONE);
            nothingFound.setText(R.string.nothing_found);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.startNestedScroll(0);
            new LoadTask<String, Void, ArrayList<Manga>>() {
                @Override
                protected ArrayList<Manga> doInBackground(String query) {
                    try{
                        return sortAdapter!=null ? Manga_Scripted.query(sortAdapter.getScript(),query,page, (Object[]) sortAdapter.getOptions()) : null;
                    }catch(Exception e){
                        onBreak(e);
                        return null;
                    }
                }
                @Override
                protected void onFinished(ArrayList<Manga> mangas) {
                    if(mangas!=null){
                        MangaService.setCacheDirIfNull(mangas);
                        if(adapter.addAll(mangas)){
                            nothingFound.setVisibility(View.GONE);
                            page++;
                            enableLoadMore=true;
                        }else{
                            enableLoadMore=false;
                        }
                    }
                    if(adapter.getItemCount()==0){nothingFound.setVisibility(View.VISIBLE);}
                    progressBar.progressiveStop();
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                protected void onBraked(Throwable e) {
                    super.onBraked(e);
                    out_error_info(e,nothingFound);
                }
            }.start(query);
        }else{
            nothingFound.setText(R.string.internet_is_loss);
            nothingFound.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.update(updateOnReturn);
        updateOnReturn=null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_search, menu);
        menu.findItem(adapter.isShowSource() ? R.id.source : R.id.status).setChecked(true);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem=menu.findItem(R.id.action_find_manga);
        searchItem.setVisible(true);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                toolbar.setTitle(query.trim());
                toolbar.collapseActionView();
                call(queryName=query.trim());
                return false;
            }
            @Override public boolean onQueryTextChange(String newText){queryName=newText.trim(); return false;}
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home: finish(); break;
            case R.id.list: item.setChecked(true); adapter.setSpanCount(1); break;
            case R.id.largeGrid: item.setChecked(true); adapter.setSpanCount(2); break;
            case R.id.mediumGrid: item.setChecked(true); adapter.setSpanCount(3); break;
            case R.id.smallGrid: item.setChecked(true); adapter.setSpanCount(4); break;
            case R.id.status:
            case R.id.source: item.setChecked(true); adapter.setShowSource(item.getItemId()==R.id.source); break;
        }
        return super.onOptionsItemSelected(item);
    }
    public static int getItemCount(RecyclerView recyclerView){return recyclerView.getLayoutManager()!=null ? recyclerView.getLayoutManager().getItemCount() : 0;}
    public static int findLastVisibleItemPosition(RecyclerView recyclerView) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        View findOneVisibleChild = findOneVisibleChild(layoutManager, layoutManager.getChildCount() - 1, -1, false, true);
        return findOneVisibleChild!=null ? recyclerView.getChildAdapterPosition(findOneVisibleChild) : -1;
    }
    private static View findOneVisibleChild(RecyclerView.LayoutManager layoutManager, int i, int i2, boolean z, boolean z2) {
        OrientationHelper orientationHelper;
        if (layoutManager.canScrollVertically()) {
            orientationHelper = OrientationHelper.createVerticalHelper(layoutManager);
        } else {
            orientationHelper = OrientationHelper.createHorizontalHelper(layoutManager);
        }
        int startAfterPadding = orientationHelper.getStartAfterPadding();
        int endAfterPadding = orientationHelper.getEndAfterPadding();
        int i3 = i2 > i ? 1 : -1;
        View view = null;
        while (i != i2) {
            View childAt = layoutManager.getChildAt(i);
            int decoratedStart = orientationHelper.getDecoratedStart(childAt);
            int decoratedEnd = orientationHelper.getDecoratedEnd(childAt);
            if (decoratedStart < endAfterPadding && decoratedEnd > startAfterPadding) {
                if (!z) {
                    return childAt;
                }
                if (decoratedStart >= startAfterPadding && decoratedEnd <= endAfterPadding) {
                    return childAt;
                }
                if (z2 && view == null) {
                    view = childAt;
                }
            }
            i += i3;
        }
        return view;
    }


    public static class FilterDialogFragment extends AppCompatDialogFragment implements View.OnClickListener{
        Button reset,apply;
        RecyclerView rv;
        final FilterSortAdapter adapter;
        private final Callback<?> callBack;
        public FilterDialogFragment(FilterSortAdapter adapter, Callback<?> callBack){
            this.adapter=adapter;
            this.callBack=callBack;
        }
        @NotNull
        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            return new BottomSheetDialog(requireContext(), Utils.Theme.isThemeDark(getContext()) ? R.style.AppDialogDark : R.style.AppDialogLight);
        }

        @Override
        public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
            return layoutInflater.inflate(R.layout.dialog_filter, viewGroup, false);
        }
        @Override
        public void onViewCreated(@NotNull View view, Bundle bundle) {
            super.onViewCreated(view, bundle);
            reset=view.findViewById(R.id.reset);
            reset.setOnClickListener(this);
            apply=view.findViewById(R.id.apply);
            apply.setOnClickListener(this);
            rv=view.findViewById(R.id.rv_list);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            rv.setAdapter(adapter);
            rv.addItemDecoration(new HeaderDividerItemDecoration(view.getContext()));
            rv.setItemAnimator(null);
        }

        public void onClick(View view) {
            switch (view.getId()){
                case R.id.apply: callBack.call(null); dismiss(); break;
                case R.id.reset: if(adapter!=null){adapter.reset();} break;
            }
        }

        public static class HeaderDividerItemDecoration extends RecyclerView.ItemDecoration{
            private final Rect bounds=new Rect();
            private final Drawable divider;

            public HeaderDividerItemDecoration(Context context){this.divider=context.getDrawable(R.color.colorLine);}

            @Override
            public void onDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.State state) {
                if (!(recyclerView.getLayoutManager() == null || this.divider == null)) {
                    int childCount = recyclerView.getChildCount();
                    for (int i = 0; i < childCount-1; i++) {
                        View childAt = recyclerView.getChildAt(i);
                        if (childAt.getTag()!=null && recyclerView.getChildAt(i+1).getTag()!=null) {
                            recyclerView.getDecoratedBoundsWithMargins(childAt, this.bounds);
                            bounds.top=bounds.bottom-divider.getIntrinsicHeight();
                            this.divider.setBounds(bounds);
                            this.divider.draw(canvas);
                        }
                    }
                }
            }
            @Override
            public void getItemOffsets(Rect rect, View view, RecyclerView recyclerView, RecyclerView.State state) {
                if (this.divider==null || view.getTag()==null) {
                    rect.set(0, 0, 0, 0);
                } else {
                    rect.set(0, 0, 0, this.divider.getIntrinsicHeight());
                }
            }
        }

    }

}