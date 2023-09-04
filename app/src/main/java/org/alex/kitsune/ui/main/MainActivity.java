package org.alex.kitsune.ui.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.navigation.NavigationView;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;
import org.alex.kitsune.Activity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.snackbar.Snackbar;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.CustomSnackbar;
import org.alex.kitsune.commons.DrawerLayout;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.book.Book_Scripted;
import org.alex.kitsune.services.BookService;
import org.alex.kitsune.ui.preview.PreviewActivity;
import org.alex.kitsune.ui.settings.SettingsActivity;
import org.alex.kitsune.ui.main.scripts.ScriptsActivity;
import org.alex.kitsune.ui.search.RecommendationsActivity;
import org.alex.kitsune.ui.search.SearchActivity;
import org.alex.kitsune.ui.shelf.PagesAdapter;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Updater;
import org.alex.kitsune.utils.Utils;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URLDecoder;


public class MainActivity extends Activity implements NavigationView.OnNavigationItemSelectedListener, MenuProvider, SwipeRefreshLayout.OnRefreshListener {
    private static final int PERMISSION_REQUEST_CODE=1;
    private static final int CALL_FILE_STORE=2;
    private static final int PERMISSION_READ_REQUEST_CODE=3;
    private String headerImagePath;
    Toolbar toolbar;
    NavigationView navigation;
    DrawerLayout drawer;
    PagesAdapter adapter;
    SmoothProgressBar progress;
    SwipeRefreshLayout swipe_refresh;
    public static boolean shouldUpdate=false;

    final SearchView.OnQueryTextListener listener=new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            if(query!=null){
                int start=query.lastIndexOf("http"),end=query.indexOf('?',start+1);
                if(start>=0){
                    query=URLDecoder.decode(query.substring(start,end!=-1?end:query.length()));
                    Book book=BookService.getOrPutNewWithDir(Book_Scripted.determinate(query.trim()));
                    if(book !=null){
                        startActivity(new Intent(MainActivity.this, PreviewActivity.class).putExtra(Constants.hash, book.hashCode()),Gravity.START,Gravity.END);
                    }else{
                        CustomSnackbar.makeSnackbar((ViewGroup) drawer.getParent(), Snackbar.LENGTH_LONG).setGravity(Gravity.CENTER).setText(getString(R.string.sourcenotdefined,query.trim())).setIcon(R.drawable.ic_caution_yellow).setBackgroundAlpha(200).show();
                    }
                }else{
                    startActivity(new Intent(MainActivity.this, SearchActivity.class).putExtra("query",query.trim()),Gravity.START,Gravity.END);
                }
            }
            return false;
        }
        @Override public boolean onQueryTextChange(String newText){return false;}
    };
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        loadLocale(this);
    }
    @Override public int getAnimationGravityIn(){return Gravity.END;}
    @Override public int getAnimationGravityOut(){return Gravity.START;}
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        adapter=new PagesAdapter(this,getSupportFragmentManager(),R.id.pager);
        headerImagePath=getExternalFilesDir(null).getAbsolutePath()+File.separator+"header";
        drawer=findViewById(R.id.drawer_layout);
        drawer.addHamburger(this,toolbar);
        drawer.addDrawerListener(new androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                drawer.getContent().setAlpha(Math.max(1-slideOffset,0.25f));
                drawer.getContent().setTranslationX(drawerView.getWidth()*slideOffset*drawer.getDirection());
            }
        });
        navigation=findViewById(R.id.nav_view);
        navigation.setCheckedItem(R.id.nav_shelf);
        navigation.setNavigationItemSelectedListener(this);
        progress=findViewById(R.id.progress);
        progress.setIndeterminateDrawable(new SmoothProgressDrawable.Builder(progress.getContext()).colors(new int[]{0xff0000ff,0xff00ffff,0xff00ff00,0xffffff00,0xffff0000,0xffff00ff}).interpolator(new AccelerateDecelerateInterpolator()).callbacks(new SmoothProgressDrawable.Callbacks() {
            @Override public void onStop(){progress.setVisibility(View.GONE);}
            @Override public void onStart(){progress.setVisibility(View.VISIBLE);}
        }).build());
        swipe_refresh=findViewById(R.id.swipe_refresh_layout);
        swipe_refresh.setColorSchemeColors(0xff0000ff,0xff00ffff,0xff00ff00,0xffffff00,0xffff0000,0xffff00ff);
        swipe_refresh.setOnRefreshListener(this);
        swipe_refresh.setProgressBackgroundColorSchemeColor(isDark()?0xff282828:0xffffffff);
        findViewById(R.id.progress).setVisibility(View.GONE);
        onNavigationItemSelected(navigation.getMenu().findItem(R.id.nav_shelf));
        getHeader(navigation).setOnClickListener(view -> callFilesStore(MainActivity.this,CALL_FILE_STORE,"image/*",PERMISSION_REQUEST_CODE));
        updateMenu();
        executeIntent(getIntent());
    }

    private static ImageView getHeader(NavigationView nav){
        return ((ImageView)nav.getHeaderView(0));
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        executeIntent(intent);
    }
    private void executeIntent(Intent intent){
        if(Intent.ACTION_VIEW.equals(intent.getAction())){
            listener.onQueryTextSubmit(intent.getDataString());
        }
    }

    public void setNew(int count){
        TextView counter=(TextView) navigation.getMenu().findItem(R.id.nav_new).getActionView();
        counter.setGravity(Gravity.CENTER_VERTICAL);
        counter.setTypeface(null, Typeface.BOLD);
        counter.setTextColor(0xffff0000);
        counter.setText(count>0 ? String.valueOf(count) : "");
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Updater.getUpdate(this,json->{
            if(json!=null){
                Updater.createSnackBarUpdate((ViewGroup) drawer.getParent()).show();
            }else if(Updater.isActualVersion()){
                Updater.checkAndUpdateScripts(this,null);
            }
        });
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onCreateMenu(@NonNull @NotNull Menu menu, @NonNull @NotNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.options_main, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_find_book).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(listener);
        if(menu instanceof MenuBuilder){((MenuBuilder)menu).setOptionalIconsVisible(true);}
    }

    @Override
    public boolean onMenuItemSelected(@NonNull @NotNull MenuItem item) {
        switch (item.getItemId()){
            case (R.id.action_add_source)->{startActivity(new Intent(this, ScriptsActivity.class),Gravity.START,Gravity.END); return true;}
            default -> {return false;}
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(shouldUpdate){
            shouldUpdate=false;
            restart(new Intent(this,MainActivity.class));
        }
        if(swipe_refresh!=null){
            swipe_refresh.setEnabled(!BookService.isAllUpdated());
            if(getSharedPreferences().getBoolean(Constants.update_on_start,true) && !BookService.isAllUpdated() && BookService.isNotCalledUpdate()){
                onRefresh();
            }
        }
        removeMenuProvider(this);
        addMenuProvider(this);
    }
    public void updateMenu(){
        if(new File(headerImagePath).exists()){
            Drawable drawable=Drawable.createFromPath(headerImagePath);
            getHeader(navigation).setImageDrawable(drawable);
            if(drawable instanceof Animatable animatable){
                animatable.start();
            }
            RecyclerView rv=((RecyclerView) navigation.getChildAt(0));
            rv.scrollToPosition(rv.getAdapter().getItemCount()-1);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_READ_REQUEST_CODE -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    callFilesStore(this, CALL_FILE_STORE, "image/*", PERMISSION_REQUEST_CODE);
                }
            }
            case PERMISSION_REQUEST_CODE -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    finish();
                    startActivity(new Intent(this, MainActivity.class),Gravity.TOP,Gravity.BOTTOM);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        switch(requestCode){
            case PERMISSION_REQUEST_CODE->{}
            case CALL_FILE_STORE->{
                if(resultCode==RESULT_OK){
                    if(data.getData()!=null){
                        try{
                            Utils.File.copy(getContentResolver().openInputStream(data.getData()),new FileOutputStream(headerImagePath));
                        }catch(Exception e){e.printStackTrace();}
                        updateMenu();
                    }
                }
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {
        switch (item.getItemId()) {
            default -> {return false;}
            case (R.id.nav_shelf) -> adapter.setCurrentItem(0);
            case (R.id.nav_new) -> adapter.setCurrentItem(1);
            case (R.id.nav_catalogs) -> adapter.setCurrentItem(2);
            case (R.id.nav_statistics) -> adapter.setCurrentItem(3);
            case (R.id.nav_recommendations) -> {startActivity(new Intent(this, RecommendationsActivity.class),Gravity.START,Gravity.END);return true;}
            case (R.id.nav_settings) -> {startActivity(new Intent(this, SettingsActivity.class),Gravity.START,Gravity.END);return true;}
            case (R.id.nav_about) -> {startActivity(new Intent(this, ActivityAbout.class),Gravity.BOTTOM,Gravity.TOP);return true;}
            case (R.id.nav_version) -> {Updater.showWhatisNew(this, false);return true;}
        }
        item.setChecked(true);
        toolbar.setTitle(item.getTitle());
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return switch (keyCode){
            case KeyEvent.KEYCODE_BACK -> {
                if(drawer.isDrawerOpen()){
                    drawer.closeDrawer();
                }else{
                    if(adapter.getCurrentIndex()==0){
                        yield super.onKeyUp(keyCode,event);
                    }else{
                        onNavigationItemSelected(navigation.getMenu().findItem(R.id.nav_shelf));
                    }
                }
                yield true;
            }
            default -> super.onKeyUp(keyCode,event);
        };
    }

    @Override
    public void onRefresh() {
        if(NetworkUtils.isNetworkAvailable(this)){
            swipe_refresh.setRefreshing(true);
            //progress.progressiveStart();
            new Handler().post(()->BookService.check_for_updates(this, n->{
                setNew(n);
                swipe_refresh.setRefreshing(false);
                //progress.progressiveStop();
                swipe_refresh.setEnabled(!BookService.isAllUpdated());
            }));
        }else{
            swipe_refresh.setRefreshing(false);
            Toast.makeText(this, R.string.no_internet,Toast.LENGTH_LONG).show();
        }
    }
}