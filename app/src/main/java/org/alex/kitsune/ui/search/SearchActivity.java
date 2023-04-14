package org.alex.kitsune.ui.search;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.commons.Callback2;
import org.alex.kitsune.commons.HttpStatusException;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.book.Book_Scripted;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.R;
import org.alex.kitsune.ui.shelf.Catalogs;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Utils;
import javax.net.ssl.SSLException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SearchActivity extends AppCompatActivity implements SearchView.OnQueryTextListener{

    Toolbar toolbar;
    RecyclerView rv;
    SourceSearchAdapter adapter;
    TextView error;
    ArrayList<Catalogs.Container> catalogs;
    String query;
    MenuItem search_item;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.Theme.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        catalogs=Catalogs.getCatalogs(PreferenceManager.getDefaultSharedPreferences(this));
        String query=getIntent().getStringExtra("query");
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        toolbar.setTitle(query);
        error=findViewById(R.id.text);
        rv=findViewById(R.id.rv_list);
        rv.setLayoutManager(new LinearLayoutManager(this,RecyclerView.VERTICAL,false));
        adapter=new SourceSearchAdapter(catalogs.stream().filter(c->c.enable).map(c->c.source).collect(Collectors.toList()),false);
        rv.setAdapter(adapter);
        rv.setVerticalScrollBarEnabled(false);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                adapter.update(intent.getIntExtra(Constants.hash,-1));
            }
        },new IntentFilter(Constants.action_Update));
        onQueryTextSubmit(query);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        search_item=menu.add("search");
        search_item.setIcon(R.drawable.ic_search);
        search_item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        SearchView searchView = new SearchView(this);
        search_item.setActionView(searchView);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(this);
        searchView.setOnSearchClickListener(v -> searchView.setQuery(query,false));
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if(!hasFocus){
                search_item.collapseActionView();
            }
        });
        if(menu instanceof MenuBuilder){((MenuBuilder)menu).setOptionalIconsVisible(true);}
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home -> finish();
        }
        return super.onOptionsItemSelected(item);
    }
    public static void search(Context context,String query,int order,List<String> sources,Callback2<String,List<Book>> callback,TextView error){
        if(NetworkUtils.isNetworkAvailable(context)){
            for(String source:sources){
                searchBook(source, query,order, callback, error);
            }
        }else{
            error.setText(R.string.internet_is_loss);
            error.setVisibility(View.VISIBLE);
        }
    }
    public static void searchBook(String source, String query, int order, Callback2<String,List<Book>> callback, TextView out_error){
        if(callback!=null){
            new Thread(()->{
                try{
                    List<Book> list=Book_Scripted.query(source,query,0,order);
                    new Handler(Looper.getMainLooper()).post(()->{
                        callback.call(source,list);
                    });
                }catch(Exception e){
                    new Handler(Looper.getMainLooper()).post(()->{
                        out_error_info(e,out_error);
                        callback.call(source,null);
                    });
                }
            }).start();
        }
    }
    public static void out_error_info(Throwable e, TextView out_error){
        if(out_error!=null){
            if(e instanceof SocketTimeoutException){
                out_error.setText(R.string.time_out);
            }else if(e instanceof HttpStatusException) {
                out_error.setText(((HttpStatusException) e).message());
            }else if(e instanceof SSLException){
                out_error.setText(e.getClass().getSimpleName());
            }else{
                out_error.setText(R.string.nothing_found);
            }
            //out_error.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        this.query=query;
        toolbar.setTitle(query);
        if(search_item!=null){search_item.collapseActionView();}
        search(this,query,0,adapter.getSources(),adapter.getCallback(),error);
        adapter.clear();
        return true;
    }
    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }
}