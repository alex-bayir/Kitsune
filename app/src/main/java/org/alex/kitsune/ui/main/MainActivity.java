package org.alex.kitsune.ui.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.preference.PreferenceManager;
import com.google.android.material.navigation.NavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.snackbar.Snackbar;
import com.soundcloud.android.crop.Crop;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.CustomSnackbar;
import org.alex.kitsune.commons.DrawerLayout;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.book.Book_Scripted;
import org.alex.kitsune.services.BookService;
import org.alex.kitsune.ui.preview.PreviewActivity;
import org.alex.kitsune.ui.settings.SettingsActivity;
import org.alex.kitsune.ui.main.scripts.ScriptsActivity;
import org.alex.kitsune.ui.search.RecommendationsActivity;
import org.alex.kitsune.ui.search.SearchActivity;
import org.alex.kitsune.ui.shelf.PagesAdapter;
import org.alex.kitsune.utils.Updater;
import org.alex.kitsune.utils.Utils;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URLDecoder;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, MenuProvider {
    private static final int PERMISSION_REQUEST_CODE=1;
    private static final int CALL_FILE_STORE=2;
    private static final int PERMISSION_READ_REQUEST_CODE=3;
    private String headerImagePath;
    Toolbar toolbar;
    NavigationView navigationView;
    DrawerLayout drawer;
    PagesAdapter adapter;
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
                        startActivity(new Intent(MainActivity.this, PreviewActivity.class).putExtra(Constants.hash, book.hashCode()));
                    }else{
                        CustomSnackbar.makeSnackbar((ViewGroup) drawer.getParent(), Snackbar.LENGTH_LONG).setGravity(Gravity.CENTER).setText(getString(R.string.sourcenotdefined,query.trim())).setIcon(R.drawable.ic_caution_yellow).setBackgroundAlpha(200).show();
                    }
                }else{
                    startActivity(new Intent(MainActivity.this, SearchActivity.class).putExtra("query",query.trim()));
                }
            }
            return false;
        }
        @Override public boolean onQueryTextChange(String newText){return false;}
    };
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        Utils.Activity.loadLocale(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.Theme.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Logs.init(this);

        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getWindow().setStatusBarColor(ContextCompat.getColor(this,R.color.transparent_dark));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this,R.color.transparent_dark));
        Updater.init(this);

        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(Constants.saved_path, BookService.init(this)).apply();
        adapter=new PagesAdapter(this.getSupportFragmentManager(),R.id.pager);
        headerImagePath=getExternalFilesDir(null).getAbsolutePath()+"/header";
        drawer=findViewById(R.id.drawer_layout);
        drawer.addHamburger(this,toolbar);
        drawer.addDrawerListener(new androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                drawer.getContent().setAlpha(Math.max(1-slideOffset,0.25f));
                drawer.getContent().setTranslationX(drawerView.getWidth()*slideOffset*drawer.getDirection());
            }
        });
        navigationView=findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        findViewById(R.id.progress).setVisibility(View.GONE);
        onNavigationItemSelected(navigationView.getMenu().findItem(R.id.nav_shelf));
        View headerImage=navigationView.getHeaderView(0);
        updateMenu();
        headerImage.setOnClickListener(view -> Utils.Activity.callFilesStore(MainActivity.this,CALL_FILE_STORE,"image/*",PERMISSION_REQUEST_CODE));
        executeIntent(getIntent());
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
        TextView counter=(TextView) navigationView.getMenu().findItem(R.id.nav_new).getActionView();
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
            case (R.id.action_add_source)->{startActivity(new Intent(this, ScriptsActivity.class)); return true;}
            default -> {return false;}
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(shouldUpdate){
            shouldUpdate=false;
            Utils.Activity.restartActivity(this,new Intent(this,MainActivity.class));
        }
        removeMenuProvider(this);
        addMenuProvider(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        File[] listF=new File(BookService.getCacheDir()).listFiles();
        if(listF!=null){for(File f:listF){Utils.File.delete(f);}}
    }
    public void updateMenu(){
        if(new File(headerImagePath).exists()){
            ((ImageView)navigationView.getHeaderView(0)).setImageDrawable(Drawable.createFromPath(headerImagePath));
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_READ_REQUEST_CODE -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Utils.Activity.callFilesStore(this, CALL_FILE_STORE, "image/*", PERMISSION_REQUEST_CODE);
                }
            }
            case PERMISSION_REQUEST_CODE -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    finish();
                    startActivity(new Intent(this, MainActivity.class));
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
                    Crop.of(data.getData(),Uri.fromFile(new File(headerImagePath))).withAspect(navigationView.getHeaderView(0).getWidth(),navigationView.getHeaderView(0).getHeight()).start(this);
                }
            }
            case Crop.REQUEST_CROP->{
                if(resultCode==RESULT_OK){
                    try{
                        Utils.File.copy(getContentResolver().openInputStream(data.getData()),new FileOutputStream(headerImagePath));
                    }catch(Exception e){e.printStackTrace();}
                    updateMenu();
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
            case (R.id.nav_recommendations) -> {startActivity(new Intent(this, RecommendationsActivity.class));return true;}
            case (R.id.nav_settings) -> {startActivity(new Intent(this, SettingsActivity.class));return true;}
            case (R.id.nav_about) -> {startActivity(new Intent(this, ActivityAbout.class));return true;}
            case (R.id.nav_version) -> {Updater.showWhatisNew(this, false);return true;}
        }
        item.setChecked(true);
        toolbar.setTitle(item.getTitle());
        return true;
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen()){
            drawer.closeDrawer();
        }else{
            if(adapter.getCurrentIndex()==0){
                super.onBackPressed();
            }else{
                onNavigationItemSelected(navigationView.getMenu().findItem(R.id.nav_shelf));
            }
        }
    }
}