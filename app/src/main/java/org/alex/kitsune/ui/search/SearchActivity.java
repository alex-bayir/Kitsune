package org.alex.kitsune.ui.search;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import fr.castorflex.android.circularprogressbar.CircularProgressBar;
import fr.castorflex.android.circularprogressbar.CircularProgressDrawable;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.commons.HttpStatusException;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.book.Book_Scripted;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.services.BookService;
import org.alex.kitsune.R;
import org.alex.kitsune.book.views.BookAdapter;
import org.alex.kitsune.ui.preview.PreviewActivity;
import org.alex.kitsune.ui.shelf.Catalogs;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Utils;
import javax.net.ssl.SSLException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    Toolbar toolbar;
    RecyclerView rv;
    BookAdapter adapter;
    HashMap<String,ArrayList<Book>> map=new HashMap<>();
    Book updateOnReturn=null;
    ArrayList<Catalogs.Container> catalogs;
    CircularProgressBar progressBar;
    TextView nothingFound;
    int sourcesFind=0;
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
        rv=findViewById(R.id.rv_list);

        adapter=new BookAdapter(null, BookAdapter.Mode.LIST, book -> {
            adapter.add(updateOnReturn= BookService.getOrPutNewWithDir(book));
            startActivity(new Intent(this, PreviewActivity.class).putExtra(Constants.hash,book.hashCode()));
        });
        adapter.setShowSource(true);
        adapter.initRV(rv,1);
        nothingFound=findViewById(R.id.text);
        progressBar=findViewById(R.id.progress);
        progressBar.setIndeterminateDrawable(new CircularProgressDrawable.Builder(this).colors(new int[]{0xff0000ff,0xff00ffff,0xff00ff00,0xffffff00,0xffff0000,0xffff00ff}).style(CircularProgressDrawable.STYLE_ROUNDED).strokeWidth(8f).sweepInterpolator(new AccelerateDecelerateInterpolator()).build());
        search(query);
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home -> finish();
            case (R.id.list) -> {item.setChecked(true); adapter.setSpanCount(1);}
            case (R.id.largeGrid) -> {item.setChecked(true); adapter.setSpanCount(2);}
            case (R.id.mediumGrid) -> {item.setChecked(true); adapter.setSpanCount(3);}
            case (R.id.smallGrid) -> {item.setChecked(true); adapter.setSpanCount(4);}
            case (R.id.status), (R.id.source) -> {item.setChecked(true); adapter.setShowSource(item.getItemId()==R.id.source);}
        }
        return super.onOptionsItemSelected(item);
    }
    public void search(String query){
        if(NetworkUtils.isNetworkAvailable(this)){
            int i=0;
            for(Catalogs.Container container:catalogs){if(container.enable){i++; searchBook(container.source, query,0, this::add, nothingFound);}}
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
    public static void searchBook(String source, String query, int order, Callback<Collection<Book>> callback, TextView out_error){
        if(callback!=null){
            new Thread(()->{
                try{
                    List<Book> list= Book_Scripted.query(source,query,0,order);
                    new Handler(Looper.getMainLooper()).post(()->{
                        callback.call(list);
                    });
                }catch(Exception e){
                    new Handler(Looper.getMainLooper()).post(()->{
                        out_error_info(e,out_error);
                        callback.call(null);
                    });
                }
            }).start();
        }
    }

    private synchronized void add(Collection<Book> books){
        sourcesFind++;
        if(books !=null && books.size()>0){
            books = books instanceof List ? books : new ArrayList<>(books);
            BookService.setCacheDirIfNull((List<Book>) books);
            adapter.addAll(books, Book.SourceComparator(Catalogs.Container.sources(catalogs)));
            progressBar.setVisibility(View.GONE);
        }
        if(sourcesFind==Catalogs.Container.countEnabled(catalogs) && adapter.getItemCount()==0){
            nothingFound.setVisibility(View.VISIBLE);
            nothingFound.setText(R.string.nothing_found);
            progressBar.setVisibility(View.GONE);
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