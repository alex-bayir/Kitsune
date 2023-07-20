package org.alex.kitsune.ui.shelf.saved;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import org.alex.kitsune.Activity;
import androidx.appcompat.widget.Toolbar;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.services.BookService;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.AppBarStateChangeListener;
import org.alex.kitsune.book.views.BookAdapter;
import org.alex.kitsune.commons.PhotoCollageDrawable;
import org.alex.kitsune.ui.preview.PreviewActivity;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Utils;
import java.util.Comparator;
import java.util.Random;


public class SavedActivity extends Activity {

    Toolbar toolbar;
    CollapsingToolbarLayout toolBarLayout;
    RecyclerView rv;
    BookAdapter adapter;
    ImageView backdrop;
    int currentSort=R.id.latest;
    final Random r=new Random();
    @Override public int getAnimationGravityIn(){return Gravity.BOTTOM;}
    @Override public int getAnimationGravityOut(){return Gravity.TOP;}
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved);
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        setColorBars();
        setActivityFullScreen();
        AppBarLayout barLayout=findViewById(R.id.app_bar);
        barLayout.addOnOffsetChangedListener(new AppBarStateChangeListener() {
            @Override
            public void onStateChanged(AppBarLayout appBarLayout, State state) {
                if(state!=State.IDLE){
                    setVisibleSystemUI(SavedActivity.this,state==State.COLLAPSED);
                    if(state==State.COLLAPSED){toolBarLayout.setExpandedTitleColor(Color.HSVToColor(new float[]{r.nextFloat()*360,1,1}));}
                }
            }
        });
        toolbar.setTitle(getString(R.string.Saved).toUpperCase());
        toolBarLayout=findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getString(R.string.Saved).toUpperCase());
        TypedValue v=new TypedValue();
        getTheme().resolveAttribute(R.attr.appBarColor,v,true);
        toolBarLayout.setContentScrimColor(v.data);
        toolBarLayout.setExpandedTitleColor(Color.HSVToColor(new float[]{r.nextFloat()*360,1,1}));
        backdrop=findViewById(R.id.backdrop);
        rv=findViewById(R.id.rv_list);
        adapter=new BookAdapter(BookService.getSorted(BookService.Type.Saved), BookAdapter.Mode.GRID, book -> startActivity(new Intent(this, PreviewActivity.class).putExtra(Constants.hash,book.hashCode()),Gravity.START,Gravity.END));
        adapter.initRV(rv,3);
        adapter.calculateFullSize();
        backdrop.setImageDrawable(createBackDrop(adapter));
        backdrop.setOnClickListener(v1 -> sharePhotoCollage((PhotoCollageDrawable) backdrop.getDrawable()));
        toolbar.setOnClickListener(v1 -> sharePhotoCollage((PhotoCollageDrawable) backdrop.getDrawable()));
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(Constants.action_Update.equals(intent.getAction())){
                    if(Constants.load.equals(intent.getStringExtra(Constants.option)) || Constants.delete.equals(intent.getStringExtra(Constants.option))){
                        Comparator<Book> comparator=switch (currentSort) {
                            default -> Book.SavingTimeComparator;
                            case (R.id.images_size) -> Book.ImagesSizesComparator;
                            case (R.id.alphabetical) -> Book.AlphabeticalComparator;
                            case (R.id.alphabetical_alt) -> Book.AlphabeticalComparatorAlt;
                        };
                        adapter.replace(BookService.getSorted(BookService.Type.Saved),comparator,true);
                        backdrop.setImageDrawable(createBackDrop(adapter));
                    }else{
                        adapter.update(BookService.get(intent.getIntExtra(Constants.hash,-1)));
                    }
                }
            }
        },new IntentFilter(Constants.action_Update));
    }
    private Drawable createBackDrop(BookAdapter adapter){
        return new PhotoCollageDrawable(AppCompatResources.getDrawable(this,R.drawable.ic_caution), 10, v->adapter.getItemCount(), i->adapter.get(i).loadThumbnail());
    }
    private boolean sharePhotoCollage(PhotoCollageDrawable drawable){
        return drawable!=null && Utils.Bitmap.shareBitmap(this,getString(R.string.Saved).toUpperCase(),drawable.getBitmap());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sorting,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.latest) -> {currentSort=item.getItemId(); item.setChecked(true); start(Book.SavingTimeComparator);}
            case (R.id.alphabetical) -> {currentSort=item.getItemId(); item.setChecked(true); start(Book.AlphabeticalComparator);}
            case (R.id.alphabetical_alt) -> {currentSort=item.getItemId(); item.setChecked(true); start(Book.AlphabeticalComparatorAlt);}
            case (R.id.images_size) -> {currentSort=item.getItemId(); item.setChecked(true); start(Book.ImagesSizesComparator);}
        }
        return super.onOptionsItemSelected(item);
    }
    private void start(Comparator<Book> comparator){
        adapter.sort(comparator, comparator==Book.ImagesSizesComparator ? 1:3);
        new Thread(()->{
            Drawable drawable=createBackDrop(adapter);
            NetworkUtils.getMainHandler().post(()->backdrop.setImageDrawable(drawable));
        }).start();
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