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
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import org.alex.kitsune.Activity;
import android.os.Bundle;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.commons.Callback2;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.book.Book_Scripted;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.R;
import org.alex.kitsune.ui.shelf.Catalogs;
import org.alex.kitsune.utils.NetworkUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SearchActivity extends Activity implements SearchView.OnQueryTextListener{

    Toolbar toolbar;
    RecyclerView rv;
    SourceSearchAdapter adapter;
    ArrayList<Catalogs.Container> catalogs;
    String query;
    MenuItem search_item;
    @Override public int getAnimationGravityIn(){return Gravity.END;}
    @Override public int getAnimationGravityOut(){return Gravity.START;}
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        catalogs=Catalogs.getCatalogs(PreferenceManager.getDefaultSharedPreferences(this));
        String query=getIntent().getStringExtra("query");
        toolbar=findViewById(R.id.toolbar);
        initActionBar(toolbar);
        toolbar.setTitle(query);
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
    public static void search(Context context,String query,int order,List<String> sources,Callback2<String,List<Book>> callback,Callback2<String,Throwable> error_callback){
        if(NetworkUtils.isNetworkAvailable(context)){
            for(String source:sources){
                searchBook(source, query,order, callback,error_callback);
            }
        }else{
            Toast.makeText(context,R.string.no_internet,Toast.LENGTH_LONG).show();
        }
    }
    public static void searchBook(String source, String query, int order, Callback2<String,List<Book>> callback,Callback2<String,Throwable> error_callback){
        if(callback!=null){
            new Thread(()->{
                try{
                    List<Book> list=Book_Scripted.query(source,query,0,order);
                    new Handler(Looper.getMainLooper()).post(()-> callback.call(source,list));
                }catch(Exception e){
                    if(error_callback!=null){
                        new Handler(Looper.getMainLooper()).post(()-> error_callback.call(source,e));
                    }
                }
            }).start();
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        this.query=query;
        toolbar.setTitle(query);
        if(search_item!=null){search_item.collapseActionView();}
        search(this,query,0,adapter.getSources(),adapter.getResultCallback(),adapter.getErrorCallback());
        adapter.clear();
        return true;
    }
    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }
}