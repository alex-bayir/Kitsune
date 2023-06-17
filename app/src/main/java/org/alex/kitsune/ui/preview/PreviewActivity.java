package org.alex.kitsune.ui.preview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
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
import com.google.android.material.textfield.TextInputLayout;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.commons.CustomSnackbar;
import java.io.FileOutputStream;
import java.io.IOException;

import org.alex.kitsune.book.Book;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.services.BookService;
import org.alex.kitsune.R;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Utils;
import java.util.List;


public class PreviewActivity extends AppCompatActivity{
    PreviewPagerAdapter adapter;
    SmoothProgressBar progressBar;
    ViewPager2 pager;
    Toolbar bottomBar;
    Toolbar toolbar;
    Book book;
    Throwable throwable=null;
    long throwableTime=0;
    SharedPreferences prefs;
    private static int hash=-1;
    static final int PERMISSION_REQUEST_CODE=1;
    static final int CALL_FILE_STORE=2;
    private Runnable scrollToHistory=()->{if(adapter.getChaptersPage()!=null){adapter.getChaptersPage().scrollToHistory(); scrollToHistory=null;}};
    private final Callback<Throwable> errorCallback=(throwable) -> {
        progressBar.progressiveStop(); this.throwable=throwable;
        if(throwable!=null && throwable.getCause() instanceof IOException e){
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
            adapter.bindPages();
        }else{
            throwableTime=Logs.saveLog(throwable);
            adapter.bindPages(throwable);
        }
        invalidateOptionsMenu();
    };
    private final Callback<Boolean> updateCallback=(updated) -> {
        toolbar.setTitle(book.getName());
        toolbar.setSubtitle(book.getNameAlt());
        if(updated){
            progressBar.progressiveStop();
            sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash, book.hashCode()));
            book.loadSimilar(obj -> {
                BookService.setCacheDirIfNull((List<Book>) obj); adapter.bindPages();},errorCallback);
            invalidateOptionsMenu();
        }
        adapter.bindPages();
        Utils.Activity.clippingToolbarTexts(toolbar,v->{createDialog(this, book).show(); return true;});
    };
    public void updateContent(){
        updateCallback.call(book.isUpdated());
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

        if(!BookService.isInited()){
            BookService.init(this);
            hash =-1;
        }
        book=BookService.getOrPutNewWithDir(getIntent().getIntExtra(Constants.hash, hash),getIntent().getStringExtra(Constants.book));
        hash=book!=null ? book.hashCode() : -1;
        if(book==null){finish();}
        if(!book.getDir().startsWith(BookService.getDir())){
            book.moveTo(BookService.getDir());}

        adapter=new PreviewPagerAdapter(book);
        pager=findViewById(R.id.pager);
        pager.setAdapter(adapter);
        progressBar.progressiveStart();
        updateContent();
        if(NetworkUtils.isNetworkAvailable(this)){
            book.update(updateCallback, errorCallback);
        }else{errorCallback.call(null);}
        new TabLayoutMediator(findViewById(R.id.tabs), pager, true, true, (tab, position) -> tab.setText(adapter.getTitle(position))).attach();
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position){
                invalidateOptionsMenu();
                switch (position) {
                    case 1 -> {
                        bottomBar.setVisibility(View.VISIBLE);
                        bottomBar.animate().translationY(0.0f).setListener(null).start();
                        if(scrollToHistory!=null){scrollToHistory.run();}
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
        getMenuInflater().inflate(R.menu.menu_preview,menu);
        menu.findItem(R.id.show_log).setVisible(throwableTime !=0);
        Utils.Menu.buildIntentSubmenu(this,new Intent(Intent.ACTION_VIEW, Uri.parse(book.getUrl_WEB())),menu.addSubMenu(0,0,100,R.string.action_open_in).setIcon(R.drawable.ic_earth));
        if(throwable!=null){menu.findItem(R.id.show_log).setVisible(throwable!=null);}
        if(menu instanceof MenuBuilder){((MenuBuilder)menu).setOptionalIconsVisible(true);}
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return switch (item.getItemId()) {
            case android.R.id.home -> {finish(); yield true;}
            case (R.id.action_open_folder) -> {startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(book.getDir()),"resource/folder")); yield true;}
            case (R.id.action_chapter_remove_all) -> {adapter.getChaptersPage().action(ChaptersPage.RA); yield true;}
            case (R.id.action_chapter_save_all) -> {adapter.getChaptersPage().action(ChaptersPage.SA); yield true;}
            case (R.id.action_chapter_remove_selected) -> {adapter.getChaptersPage().action(ChaptersPage.RS); yield true;}
            case (R.id.action_chapter_save_selected) -> {adapter.getChaptersPage().action(ChaptersPage.SS); yield true;}
            case (R.id.action_reverse) -> {
                boolean z = adapter.getChaptersPage().setReversed(!item.isChecked());
                item.setChecked(z).setIcon(z ? R.drawable.ic_sort_numeric_reverse : R.drawable.ic_sort_numeric);
                prefs.edit().putBoolean(Constants.reversed, z).apply();
                yield z;
            }
            case (R.id.show_log) -> {Logs.createDialog(this, throwable, throwableTime).show(); yield true;}
            case (R.id.action_share) -> {startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, book.getUrl()).setType("text/plain").putExtra(Intent.EXTRA_TITLE, book.getAnyName()).putExtra(Intent.EXTRA_SUBJECT, book.getAnyName()), null)); yield true;}
            case (R.id.action_create_shortcut) -> {
                if (createShortCutBook(book)) {
                    CustomSnackbar.makeSnackbar(findViewById(android.R.id.content), Snackbar.LENGTH_LONG).setGravity(Gravity.TOP | Gravity.CENTER_VERTICAL).setText(R.string.if_shortcut_did_not_created).setIcon(R.drawable.ic_caution_yellow).setAction(R.string.settings, v -> Utils.App.callPermissionsScreen(this, Settings.ACTION_APPLICATION_DETAILS_SETTINGS)).setBackgroundAlpha(200).show();
                } else {
                    Toast.makeText(this, R.string.unable_create_shortcut, Toast.LENGTH_LONG).show();
                }
                yield true;
            }
            default -> super.onOptionsItemSelected(item);
        };
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
                adapter.getChaptersPage().search(PreviewActivity.this,newText);
                return false;
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        switch(requestCode){
            case PERMISSION_REQUEST_CODE: break;
            case CALL_FILE_STORE:
                if(resultCode==RESULT_OK){
                    try{Utils.File.copy(getContentResolver().openInputStream(data.getData()),new FileOutputStream(book.getCoverPath()));}catch(Exception e){e.printStackTrace();}
                    updateContent();
                }
                break;
        }
    }
    public void setSelectMode(boolean select_mode){
        Menu menu=bottomBar.getMenu();
        menu.findItem(R.id.action_chapter_remove_all).setVisible(!select_mode);
        menu.findItem(R.id.action_chapter_remove_selected).setVisible(select_mode);
        menu.findItem(R.id.action_chapter_save_all).setVisible(!select_mode);
        menu.findItem(R.id.action_chapter_save_selected).setVisible(select_mode);
    }
    public boolean createShortCutBook(Book book){
        return Utils.createShortCutPreview(this, book.hashCode(), book.getAnyName(), book.getCoverPath(),new Intent(Intent.ACTION_VIEW,null,this, PreviewActivity.class).putExtra(Constants.hash, book.hashCode()).putExtra(Constants.book, book.toString()));
    }
    private Dialog createDialog(Context context, Book book){
        View view=LayoutInflater.from(context).inflate(R.layout.dialog_edit_names,null);
        Dialog changeNames=new AlertDialog.Builder(context).setView(view).create();
        EditText name=view.findViewById(R.id.input_name);
        EditText name_alt=view.findViewById(R.id.input_name_alt);
        name.setText(book.getName());
        name_alt.setText(book.getNameAlt());
        ((TextInputLayout) view.findViewById(R.id.input_name_layout)).setEndIconOnClickListener(v -> {
            Utils.setClipboard(v.getContext(),name.getText().toString());
            Toast.makeText(v.getContext(),name.getText().toString(),Toast.LENGTH_SHORT).show();
        });
        ((TextInputLayout) view.findViewById(R.id.input_name_alt_layout)).setEndIconOnClickListener(v -> {
            Utils.setClipboard(v.getContext(),name_alt.getText().toString());
            Toast.makeText(v.getContext(),name_alt.getText().toString(),Toast.LENGTH_SHORT).show();
        });
        view.findViewById(R.id.close).setOnClickListener(v -> changeNames.cancel());
        view.findViewById(R.id.save).setOnClickListener(v -> {
            boolean canSave=true;
            if(name.getText()==null || name.getText().length()==0){
                name.setError("Length must more than zero"); canSave=false;
            }
            if(name_alt.getText()==null || name_alt.getText().length()==0){
                name_alt.setError("Length must more than zero"); canSave=false;
            }
            if(canSave){
                book.setNames(name.getText().toString(),name_alt.getText().toString());
                changeNames.cancel();
                updateContent();
            }
        });
        view.findViewById(R.id.discard).setOnClickListener(v->{
            book.setEdited(false);
            changeNames.cancel();
        });
        return changeNames;
    }
}