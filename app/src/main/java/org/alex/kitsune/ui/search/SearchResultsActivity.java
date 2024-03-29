package org.alex.kitsune.ui.search;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import org.alex.kitsune.Activity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.R;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.book.views.BookAdapter;
import org.alex.kitsune.services.BookService;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.ui.preview.PreviewActivity;
import org.alex.kitsune.utils.Binder;

import java.util.List;

public class SearchResultsActivity extends Activity {
    Toolbar toolbar;
    RecyclerView rv;
    BookAdapter adapter;
    @Override public int getAnimationGravityIn(){return Gravity.END;}
    @Override public int getAnimationGravityOut(){return Gravity.START;}
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        String source=getIntent().getStringExtra("source");
        List<Book> books=getIntent().getExtras().getBinder("books") instanceof Binder<?> binder? (List<Book>) binder.getData():null;
        toolbar=findViewById(R.id.toolbar);
        initActionBar(toolbar);
        toolbar.setTitle(source);
        rv=findViewById(R.id.rv_list);
        adapter=new BookAdapter(books, BookAdapter.Mode.LIST, book -> {
            adapter.add(BookService.getOrPutNewWithDir(book));
            startActivity(new Intent(this, PreviewActivity.class).putExtra(Constants.hash,book.hashCode()),Gravity.START,Gravity.END);
        });
        adapter.initRV(rv,1);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                adapter.update(BookService.get(intent.getIntExtra(Constants.hash,-1)));
            }
        },new IntentFilter(Constants.action_Update));
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
            case (R.id.list) -> {item.setChecked(true); adapter.setSpanCount(1);}
            case (R.id.largeGrid) -> {item.setChecked(true); adapter.setSpanCount(2);}
            case (R.id.mediumGrid) -> {item.setChecked(true); adapter.setSpanCount(3);}
            case (R.id.smallGrid) -> {item.setChecked(true); adapter.setSpanCount(4);}
            case (R.id.status), (R.id.source) -> {item.setChecked(true); adapter.setShowSource(item.getItemId()==R.id.source);}
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onResume() {
        super.onResume();
        if(adapter!=null){
            adapter.setEnableUpdate(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(adapter!=null){
            adapter.setEnableUpdate(false);
        }
    }
}