package org.alex.kitsune.ui.search;

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
import androidx.appcompat.widget.Toolbar;
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

public class SearchActivity extends AppCompatActivity {

    Toolbar toolbar;
    RecyclerView rv;
    SourceSearchAdapter adapter;
    TextView error;
    ArrayList<Catalogs.Container> catalogs;
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
        search(this,query,0,adapter.getSources(),adapter.getCallback(),error);
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
}