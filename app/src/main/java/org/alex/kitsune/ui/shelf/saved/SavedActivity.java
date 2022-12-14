package org.alex.kitsune.ui.shelf.saved;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.TypedValue;
import android.view.*;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.services.MangaService;
import org.alex.kitsune.R;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.commons.AppBarStateChangeListener;
import org.alex.kitsune.manga.views.MangaAdapter;
import org.alex.kitsune.commons.PhotoCollageDrawable;
import org.alex.kitsune.ui.preview.PreviewActivity;
import org.alex.kitsune.utils.LoadTask;
import org.alex.kitsune.utils.Utils;
import java.util.Comparator;
import java.util.Random;


public class SavedActivity extends AppCompatActivity {

    Toolbar toolbar;
    CollapsingToolbarLayout toolBarLayout;
    RecyclerView rv;
    MangaAdapter adapter;
    ImageView backdrop;
    int currentSort=R.id.latest;
    final Random r=new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.Theme.getTheme(this));
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
        Utils.Activity.setColorBars(this,0,Utils.Theme.isThemeDark(this) ? 0 : getWindow().getStatusBarColor());
        Utils.Activity.setActivityFullScreen(this);
        AppBarLayout barLayout=findViewById(R.id.app_bar);
        barLayout.addOnOffsetChangedListener(new AppBarStateChangeListener() {
            @Override
            public void onStateChanged(AppBarLayout appBarLayout, State state) {
                if(state!=State.IDLE){
                    Utils.Activity.setVisibleSystemUI(SavedActivity.this,state==State.COLLAPSED);
                    if(state==State.COLLAPSED){toolBarLayout.setExpandedTitleColor(Color.HSVToColor(new float[]{r.nextFloat()*360,1,1}));}
                }
            }
        });
        toolbar.setTitle(getString(R.string.Saved_manga).toUpperCase());
        toolBarLayout=findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getString(R.string.Saved_manga).toUpperCase());
        TypedValue v=new TypedValue();
        getTheme().resolveAttribute(R.attr.appBarColor,v,true);
        toolBarLayout.setContentScrimColor(v.data);
        toolBarLayout.setExpandedTitleColor(Color.HSVToColor(new float[]{r.nextFloat()*360,1,1}));
        backdrop=findViewById(R.id.backdrop);
        rv=findViewById(R.id.rv_list);
        adapter=new MangaAdapter(MangaService.getSorted(MangaService.Type.Saved), MangaAdapter.Mode.GRID, manga -> startActivity(new Intent(this, PreviewActivity.class).putExtra(Constants.hash,manga.hashCode())));
        adapter.initRV(rv,3);
        adapter.recalculateFullSize();
        backdrop.setImageDrawable(createBackDrop(adapter));
        backdrop.setOnClickListener(v1 -> sharePhotoCollage((PhotoCollageDrawable) backdrop.getDrawable()));
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(Constants.action_Update.equals(intent.getAction())){
                    Manga manga=MangaService.get(intent.getIntExtra(Constants.hash,-1));
                    if(Constants.load.equals(intent.getStringExtra(Constants.option)) || Constants.delete.equals(intent.getStringExtra(Constants.option))){
                        if(manga.countSaved()>0){
                            switch (currentSort){
                                case R.id.latest: adapter.add(0,manga); break;
                                case R.id.images_size: adapter.addBySize(manga); break;
                                case R.id.alphabetical: adapter.add(manga,Manga.AlphabeticalComparator); break;
                            }
                            adapter.update(manga);
                        }else{
                            adapter.remove(manga);
                        }
                        backdrop.setImageDrawable(createBackDrop(adapter));
                    }else{
                        adapter.update(manga);
                    }
                }
            }
        },new IntentFilter(Constants.action_Update));
    }
    private Drawable createBackDrop(MangaAdapter adapter){
        return new PhotoCollageDrawable(getDrawable(R.drawable.ic_caution), 10, v->adapter.getItemCount(), i->adapter.get(i).loadThumbnail());
    }
    private boolean sharePhotoCollage(PhotoCollageDrawable drawable){
        return drawable!=null && Utils.Bitmap.shareBitmap(this,getString(R.string.Saved_manga).toUpperCase(),drawable.getBitmap());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sorting,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home: finish(); break;
            case R.id.latest:       currentSort=item.getItemId(); item.setChecked(true); start(Manga.SavingTimeComparator); break;
            case R.id.alphabetical: currentSort=item.getItemId(); item.setChecked(true); start(Manga.AlphabeticalComparator); break;
            case R.id.images_size:  currentSort=item.getItemId(); item.setChecked(true); start(Manga.ImagesSizesComparator); break;
        }
        return super.onOptionsItemSelected(item);
    }
    private void start(Comparator<Manga> comparator){
        adapter.sort(comparator, comparator==Manga.ImagesSizesComparator ? 1:3);
        new Thread(()->
            new Handler(Looper.getMainLooper()){
                @Override public void handleMessage(@NonNull Message msg) {
                    backdrop.setImageDrawable((Drawable) msg.obj);
                }
            }.sendMessage(LoadTask.message(createBackDrop(adapter)))
        ).start();
    }
}