package org.alex.kitsune.ui.preview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.commons.CustomSnackbar;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.services.MangaService;
import org.alex.kitsune.R;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.utils.Utils;
import java.util.List;


public class PreviewActivity extends AppCompatActivity{
    PreviewPagerAdapter adapter;
    SmoothProgressBar progressBar;
    ViewPager2 pager;
    Toolbar bottomBar;
    Toolbar toolbar;
    Manga manga;
    Throwable throwable=null;
    long throwableTime=0;
    SharedPreferences prefs;
    private static int hashManga=-1;
    private final Callback<Throwable> errorCallback=(throwable) -> {progressBar.progressiveStop(); adapter.bindPages(throwable); throwableTime=Logs.saveLog(throwable); this.throwable=throwable; invalidateOptionsMenu();};
    private void updateContent(){
        toolbar.setTitle(manga.getName());
        toolbar.setSubtitle(manga.getNameAlt());
        if(manga.isUpdated()){
            progressBar.progressiveStop();
            sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash,manga.hashCode()));
            manga.loadSimilar(obj -> {MangaService.setCacheDirIfNull((List<Manga>) obj); adapter.bindPages();},errorCallback);
            invalidateOptionsMenu();
        }
        Utils.Activity.clippingToolbarTexts(toolbar);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logs.init(this);
        Thread.setDefaultUncaughtExceptionHandler((paramThread, paramThrowable) -> {
            Logs.saveLog(paramThrowable);
            System.exit(2);
        });
        setTheme(Utils.Theme.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        toolbar=findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        prefs=PreferenceManager.getDefaultSharedPreferences(this);

        progressBar=findViewById(R.id.progress);
        progressBar.setIndeterminateDrawable(new SmoothProgressDrawable.Builder(this).colors(new int[]{0xff0000ff,0xff00ffff,0xff00ff00,0xffffff00,0xffff0000,0xffff00ff}).interpolator(new AccelerateDecelerateInterpolator()).callbacks(new SmoothProgressDrawable.Callbacks() {
            @Override public void onStop(){progressBar.setVisibility(View.GONE);}
            @Override public void onStart(){progressBar.setVisibility(View.VISIBLE);}
        }).build());
        bottomBar=findViewById(R.id.bottomBar);
        bottomBar.inflateMenu(R.menu.options_preview_bottombar);
        bottomBar.setOnMenuItemClickListener(PreviewActivity.this::onOptionsItemSelected);
        onPrepareBottomBarMenu(bottomBar.getMenu());
        bottomBar.setBackgroundColor(ContextCompat.getColor(this,R.color.black_overlay));
        Utils.Activity.setColorBars(this,getWindow().getStatusBarColor(),Utils.Theme.isThemeDark(this) ? 0 : getWindow().getStatusBarColor());

        if(!MangaService.isInited()){
            MangaService.init(this);
            hashManga=-1;
        }
        manga=MangaService.getOrPutNewWithDir(getIntent().getIntExtra(Constants.hash,hashManga),Manga.fromJSON(getIntent().getStringExtra(Constants.manga)));
        hashManga=manga!=null ? manga.hashCode() : -1;
        if(manga==null){finish();}
        if(!manga.getDir().startsWith(MangaService.getDir())){manga.moveTo(MangaService.getDir());}

        adapter=new PreviewPagerAdapter(this,manga);
        pager=findViewById(R.id.pager);
        pager.setAdapter(adapter);
        progressBar.progressiveStart();
        updateContent();
        manga.update(this, this::updateContent, errorCallback);
        new TabLayoutMediator(findViewById(R.id.tabs), pager, true, true, (tab, position) -> tab.setText(adapter.getTitle(position))).attach();
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position){
                invalidateOptionsMenu();
                switch (position) {
                    case 1 -> {
                        bottomBar.setVisibility(View.VISIBLE);
                        bottomBar.animate().translationY(0.0f).setListener(null).start();
                    }
                    default ->
                            bottomBar.animate().translationY((float) bottomBar.getHeight()).setListener(new AnimatorListenerAdapter() {
                                public void onAnimationEnd(Animator animator) {
                                    bottomBar.setVisibility(View.GONE);
                                }
                            }).start();
                }
            }
        });
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(Constants.action_Update.equals(intent.getAction())){
                    adapter.bindPages();
                }
            }
        },new IntentFilter(Constants.action_Update));
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_preview_activity,menu);
        menu.findItem(R.id.show_log).setVisible(throwableTime !=0);
        Utils.Menu.buildIntentSubmenu(this,new Intent(Intent.ACTION_VIEW, Uri.parse(manga.getUrl_WEB())),menu.addSubMenu(R.string.action_open_in).setIcon(R.drawable.ic_earth));
        if(throwable!=null){menu.findItem(R.id.show_log).setVisible(throwable!=null);}
        if(menu instanceof MenuBuilder){((MenuBuilder)menu).setOptionalIconsVisible(true);}
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(adapter!=null){adapter.bindPages();}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home: finish(); break;
            case R.id.action_chapter_remove_all: adapter.getChaptersPage().action(ChaptersPage.RA); return true;
            case R.id.action_chapter_save_all: adapter.getChaptersPage().action(ChaptersPage.SA); return true;
            case R.id.action_chapter_remove_selected: adapter.getChaptersPage().action(ChaptersPage.RS); return true;
            case R.id.action_chapter_save_selected: adapter.getChaptersPage().action(ChaptersPage.SS); return true;
            case R.id.action_reverse:
                boolean z=adapter.getChaptersPage().setReversed(!item.isChecked());
                item.setChecked(z).setIcon(z ? R.drawable.ic_sort_numeric_reverse : R.drawable.ic_sort_numeric);
                prefs.edit().putBoolean(Constants.reversed,z).apply();
                return z;
            case R.id.show_log: Logs.createDialog(this, throwable, throwableTime).show(); break;
            case R.id.action_share: startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT,manga.getUrl()).setType("text/plain").putExtra(Intent.EXTRA_TITLE,manga.getAnyName()).putExtra(Intent.EXTRA_SUBJECT,manga.getAnyName()),null)); break;
            case R.id.action_create_shortcut: if(createShortCutManga(manga)){CustomSnackbar.makeSnackbar(findViewById(android.R.id.content),Snackbar.LENGTH_LONG).setGravity(Gravity.TOP|Gravity.CENTER_VERTICAL).setText(R.string.if_shortcut_did_not_created).setIcon(R.drawable.ic_caution_yellow).setAction(R.string.settings,v -> Utils.App.callPermissionsScreen(this, Settings.ACTION_APPLICATION_DETAILS_SETTINGS)).setBackgroundAlpha(200).show();}else{Toast.makeText(this,R.string.unable_create_shortcut,Toast.LENGTH_LONG).show();} break;
            default: return super.onOptionsItemSelected(item);
        }
        return true;
    }
    private void onPrepareBottomBarMenu(Menu menu) {
        menu.findItem(R.id.action_reverse).setChecked(prefs.getBoolean(Constants.reversed,false)).setIcon(menu.findItem(R.id.action_reverse).isChecked() ? R.drawable.ic_sort_numeric_reverse : R.drawable.ic_sort_numeric);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_find_chapter).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query){return false;}
            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getChaptersPage().search(newText);
                return false;
            }
        });
    }
    public void setSelectMode(boolean select_mode){
        Menu menu=bottomBar.getMenu();
        menu.findItem(R.id.action_chapter_remove_all).setVisible(!select_mode);
        menu.findItem(R.id.action_chapter_remove_selected).setVisible(select_mode);
        menu.findItem(R.id.action_chapter_save_all).setVisible(!select_mode);
        menu.findItem(R.id.action_chapter_save_selected).setVisible(select_mode);
    }
    public boolean createShortCutManga(Manga manga){
        return Utils.createShortCutPreview(this,manga.hashCode(),manga.getAnyName(),manga.getCoverPath(),new Intent(Intent.ACTION_VIEW,null,this, PreviewActivity.class).putExtra(Constants.hash,manga.hashCode()).putExtra(Constants.manga,manga.toString()));
    }
}