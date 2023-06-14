package org.alex.kitsune.ui.shelf.history;


import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.services.BookService;
import org.alex.kitsune.R;
import org.alex.kitsune.book.views.BookAdapter;
import org.alex.kitsune.commons.SwipeRemoveHelper;
import org.alex.kitsune.ui.preview.PreviewActivity;
import org.alex.kitsune.utils.Utils;

public class HistoryActivity extends AppCompatActivity {

    Toolbar toolbar;
    RecyclerView rv;
    BookAdapter adapter;
    Snackbar sn=null;
    Book tmp=null,updateOnReturn=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.Theme.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        toolbar.setTitle(R.string.History);
        rv=findViewById(R.id.rv_list);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter=new BookAdapter(BookService.getSorted(BookService.Type.History), BookAdapter.Mode.LIST, book -> {startActivity(new Intent(this, PreviewActivity.class).putExtra(Constants.hash,book.hashCode())); updateOnReturn=book;});
        adapter.initRV(rv,1);
        SwipeRemoveHelper.setup(rv,new SwipeRemoveHelper(HistoryActivity.this,R.color.error,R.drawable.ic_trash_white,24, i -> {
            sn=Snackbar.make(toolbar,"Clear History: "+adapter.get(i).getName(),Snackbar.LENGTH_LONG).setAction("Cancel", v -> {if(sn!=null){sn.dismiss();}});
            sn.addCallback(new Snackbar.Callback(){
                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {
                    switch (event){
                        case Snackbar.Callback.DISMISS_EVENT_MANUAL: adapter.add(tmp, Book.HistoryComparator); tmp=null; break;
                        case Snackbar.Callback.DISMISS_EVENT_TIMEOUT: clear(); break;
                    }
                }
            });
            sn.show();
            clear();
            tmp=adapter.remove(i);
        }));

    }

    public void clear(){
        if(tmp!=null){
            tmp.clearHistory();
            BookService.allocate(tmp,true);
            sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash,tmp.hashCode()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.add(updateOnReturn, Book.HistoryComparator);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sorting_source, menu);
        menu.findItem(adapter.isShowSource() ? R.id.source : R.id.status).setChecked(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home -> finish();
            case (R.id.latest) -> {item.setChecked(true);adapter.sort(Book.HistoryComparator,true);}
            case (R.id.alphabetical) -> {item.setChecked(true);adapter.sort(Book.AlphabeticalComparator,true);}
            case (R.id.alphabetical_alt) -> {item.setChecked(true);adapter.sort(Book.AlphabeticalComparatorAlt,true);}
            case (R.id.status),(R.id.source) -> {item.setChecked(true);adapter.setShowSource(item.getItemId()==R.id.source);}
        }
        return super.onOptionsItemSelected(item);
    }
}