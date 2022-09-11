package org.alex.kitsune.ui.reader;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.*;
import android.view.animation.LinearInterpolator;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.services.MangaService;
import org.alex.kitsune.R;
import org.alex.kitsune.ui.shelf.StatisticsFragment;
import org.alex.kitsune.utils.Utils.Translator;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.ui.settings.SettingsActivity;
import org.alex.kitsune.manga.BookMark;
import org.alex.kitsune.manga.Chapter;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.ui.preview.CustomAdapter;
import org.alex.kitsune.commons.HolderListener;
import org.alex.kitsune.utils.*;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.util.Map;

public class ReaderActivity extends AppCompatActivity implements View.OnSystemUiVisibilityChangeListener,SeekBar.OnSeekBarChangeListener{

    View root,frame;
    ReaderStatusBar statusBar;
    Toolbar toolBar;
    View bottomBar;
    SeekBar seekBar;
    TextView progress_text;
    TextView showProgress;
    RecyclerView reader;
    Dialog Chapters, FailedLoadPagesInfo;
    ReaderPageAdapter adapter;
    Manga manga;
    Chapter chapter;
    private int num_chapter;
    private int page=0;
    public static ImageCache imageCache;
    View menu_button,translate;
    Button btn_next;
    RecyclerView rv;
    CustomAdapter<Chapter> chaptersAdapter;
    SharedPreferences prefs;
    boolean useVolumeKeys;
    float lastBrightness;
    Throwable lastThrowable;
    long start_read_time;

    private void setData(int num_chapter){
        chapter=manga.getChapters().get(num_chapter);
        if(chapter!=null){
            toolBar.setTitle(manga.getName());
            toolBar.setSubtitle(chapter.text(toolBar.getContext()));
            if(chapter.getPages()!=null && chapter.getPages().size()>0){
                seekBar.setMax(chapter.getPages().size()-1);
                adapter.setChapter(num_chapter);
                if(page==-1){page=chapter.getPages().size()-1;}
                reader.scrollToPosition(page);
                manga.createHistory(chapter,page);
                manga.seen(num_chapter);
                seekBar.setProgress(page);
            }else{
                seekBar.setMax(-1);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull @NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(Constants.hash,manga!=null ? manga.hashCode():-1);
        outState.putBoolean(Constants.history,true);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedState) {
        setTheme(Utils.Theme.getTheme(this));
        super.onCreate(savedState);
        setContentView(R.layout.activity_reader);
        lastBrightness=getBrightness();
        prefs=PreferenceManager.getDefaultSharedPreferences(this);
        Intent intent=getIntent();
        manga=MangaService.get(intent.getIntExtra(Constants.hash,savedState!=null ? savedState.getInt(Constants.hash,-1):-1));
        translate=findViewById(R.id.action_translate);
        translate.setOnTouchListener((v, event) -> {
            switch(event.getAction()){
                case MotionEvent.ACTION_DOWN: v.animate().scaleY(1.5f).scaleX(1.5f).setDuration(200).setInterpolator(new LinearInterpolator()).setListener(null).start(); break;
                case MotionEvent.ACTION_UP: v.animate().scaleY(1).scaleX(1).setDuration(200).setInterpolator(new LinearInterpolator()).setListener(null).start(); break;
                case MotionEvent.ACTION_MOVE: v.setPressed(false); break;
            }
            return false;
        });
        translate.setOnClickListener(v ->{
            Map<String, ResolveInfo> table=Translator.getTranslators(ReaderActivity.this);
            if(table.size()>1){
                PopupMenu popupMenu=new PopupMenu(ReaderActivity.this, translate,Gravity.END);
                for(Map.Entry<String,ResolveInfo> entry:table.entrySet()){
                    popupMenu.getMenu().add(entry.getValue().loadLabel(getPackageManager())).setIcon(entry.getValue().loadIcon(getPackageManager())).setOnMenuItemClickListener(item -> {
                        new Thread(()->Translator.callTranslator(ReaderActivity.this,Utils.Bitmap.saveBitmap(Utils.Bitmap.screenView(reader), Bitmap.CompressFormat.JPEG,new File(getExternalCacheDir()+File.separator+"tmp.jpg")),entry.getValue().activityInfo)).start(); return false;
                    });
                }
                popupMenu.setForceShowIcon(true);
                popupMenu.show();
            }else{
                new Thread(() -> Translator.callTranslator(ReaderActivity.this,Utils.Bitmap.saveBitmap(Utils.Bitmap.screenView(reader), Bitmap.CompressFormat.JPEG,new File(getExternalCacheDir()+File.separator+"tmp.jpg")),table)).start();
            }
        });
        reader=findViewById(R.id.reader);
        adapter=new ReaderPageAdapter(manga,reader, v -> Utils.Activity.inverseVisibleSystemUI(this), v -> {if(reader.getLayoutDirection()!=View.LAYOUT_DIRECTION_RTL){prev();}else{next();}}, v -> {if(reader.getLayoutDirection()!=View.LAYOUT_DIRECTION_RTL){next();}else{prev();}});
        reader.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull @NotNull RecyclerView recyclerView, int newState) {
                seekBar.setProgress(page=adapter.getLayoutManager().findLastVisibleItemPosition());
            }
        });
        num_chapter=intent.getIntExtra(Constants.chapter,0);
        int num_bookMark=intent.getIntExtra(Constants.bookmark,-1);
        if(intent.getBooleanExtra(Constants.history,savedState!=null && savedState.getBoolean(Constants.history,false)) && manga.getHistory()!=null){
            num_chapter=manga.getNumChapterHistory();
            page=manga.getHistory().getPage();
        }
        if(num_bookMark>=0 && manga.getBookMarks()!=null && num_bookMark<manga.getBookMarks().size()){
            final BookMark bookMark=manga.getBookMarks().get(num_bookMark);
            num_chapter=manga.getNumChapter(bookMark);
            page=bookMark.getPage();
            if(num_chapter<0){
                manga.update(this, () -> {
                    manga.updateDetails();
                    num_chapter=manga.getNumChapter(bookMark.getChapter());
                    page=bookMark.getPage();
                    initChapter(num_chapter,page);
                },null);
            }
        }
        int color=ContextCompat.getColor(this,R.color.transparent_dark);
        Utils.Activity.setColorBars(this,color,color);
        Utils.Activity.setActivityFullScreen(this);
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);
        statusBar=findViewById(R.id.statusBar);
        statusBar.setIsActive(true);
        toolBar=findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        bottomBar=findViewById(R.id.bottomBar);
        seekBar=findViewById(R.id.progress);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setOnTouchListener((v, event) -> {if(event.getAction()==MotionEvent.ACTION_UP){page=Math.max(0,Math.min(seekBar.getMax(),seekBar.getProgress()));}return false;});
        progress_text=findViewById(R.id.progress_text);
        showProgress=findViewById(R.id.SHOW_PROGRESS);
        root=findViewById(R.id.root);
        frame=findViewById(R.id.frame);
        imageCache=new ImageCache(20 * 100 * 1024 * 1024);
        menu_button=findViewById(R.id.action_menu);
        menu_button.setOnClickListener(v -> {
            PopupMenu popupMenu=new PopupMenu(ReaderActivity.this, menu_button,Gravity.START);
            getMenuInflater().inflate(R.menu.options_reader,popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(this::onOptionsItemSelected);
            popupMenu.setForceShowIcon(true);
            popupMenu.show();
        });
        View view=getLayoutInflater().inflate(R.layout.dialog_reader_error,null);
        view.findViewById(R.id.close).setOnClickListener(v -> {
            FailedLoadPagesInfo.dismiss();
            finish();
        });
        view.findViewById(R.id.retry).setOnClickListener(v -> {
            if(NetworkUtils.isNetworkAvailable(this)){
                FailedLoadPagesInfo.dismiss();
                initChapter(num_chapter,page);
            }else{
                Toast.makeText(this,R.string.no_internet,Toast.LENGTH_SHORT).show();
            }
        });
        view.findViewById(R.id.error).setOnClickListener(v-> Logs.createDialog(v.getContext(),lastThrowable).show());

        FailedLoadPagesInfo=new AlertDialog.Builder(this).setView(view).create();
        FailedLoadPagesInfo.setOnShowListener(dialogInterface -> FailedLoadPagesInfo.requireViewById(R.id.error).setVisibility(lastThrowable!=null ? View.VISIBLE : View.INVISIBLE));
        FailedLoadPagesInfo.setCancelable(false);
        view=getLayoutInflater().inflate(R.layout.dialog_chapters,null);
        rv=view.findViewById(R.id.rv_list);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.addItemDecoration(new DividerItemDecoration(rv.getContext(), DividerItemDecoration.VERTICAL));
        chaptersAdapter=new CustomAdapter<>(this, manga, manga.getChapters(), R.layout.item_chapter, new HolderListener() {
            @Override
            public void onItemClick(View v, int index){
                initChapter(index,0);
                Chapters.cancel();
            }
            @Override public boolean onItemLongClick(View v, int index){return false;}
        },null);
        rv.setAdapter(chaptersAdapter);
        view.findViewById(R.id.close).setOnClickListener(v1 -> Chapters.cancel());
        btn_next=view.findViewById(R.id.next);
        btn_next.setOnClickListener(v2 -> {
            initChapter(num_chapter+1,0);
            Chapters.cancel();
        });
        Chapters=new AlertDialog.Builder(this).setView(view).create();
        initChapter(num_chapter,page);
    }

    public void next(){
        if(page+1<chapter.getPages().size()){
            reader.scrollToPosition(++page);
            seekBar.setProgress(page);
        }else{
            initChapter(num_chapter+1,0);
        }
    }
    public void prev(){
        if(page>0){
            reader.scrollToPosition(--page);
            seekBar.setProgress(page);
        }else{
            initChapter(num_chapter-1,-1);
        }
    }


    public void initChapter(int num_chapter,int page){
        if(num_chapter<manga.getChapters().size() && num_chapter>=0){
            this.num_chapter=num_chapter;
            this.page=page;
            if(manga.checkChapterInfo(num_chapter)){
                setData(num_chapter);
            }else{
                if(NetworkUtils.isNetworkAvailable(this)){
                    lastThrowable=null;
                    new LoadTask<Manga,Void,Integer>(){
                        @Override
                        public Integer doInBackground(Manga manga) {
                            try{return manga.getPages(num_chapter).size();}catch(Exception e){Logs.saveLog(lastThrowable=e); return -1;}
                        }
                        @Override
                        public void onFinished(Integer integer) {
                            if(integer>0){setData(num_chapter);}
                            else{FailedLoadPagesInfo.show();}
                        }
                    }.start(manga);
                }else{
                    FailedLoadPagesInfo.show();
                    setData(num_chapter);
                }
            }
        }
    }

    public void setModes(int modeR, int modeS){
        prefs.edit().putInt(Constants.ReaderMode,modeR).putInt(Constants.ScaleMode,modeS).apply();
        adapter.setModes(modeR,modeS);
        seekBar.setLayoutDirection(reader.getLayoutDirection());
    }

    @Override
    protected void onResume() {
        super.onResume();
        useVolumeKeys=prefs.getBoolean(Constants.use_volume_keys,true);
        setModes(prefs.getInt(Constants.ReaderMode,0),prefs.getInt(Constants.ScaleMode,ReaderPageHolder.ScaleType.FIT_X.ordinal()));
        frame.setBackgroundColor(prefs.getBoolean(Constants.custom_background,false) ? prefs.getInt(Constants.custom_background_color,0) : 0);
        statusBar.setIsActive(prefs.getBoolean(Constants.show_status_bar,true));
        reader.setKeepScreenOn(prefs.getBoolean(Constants.keep_screen_on,false));
        setBrightness(!prefs.getBoolean(Constants.adjust_brightness,false),prefs.getInt(Constants.adjust_brightness_value,50));
        start_read_time=System.currentTimeMillis();
    }

    @Override
    protected void onStop() {
        super.onStop();
        imageCache.evictAll();
        ReaderPageHolder.clearLocks();
        manga.createHistory(chapter,page);
        MangaService.allocate(manga,false);
        sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash,manga.hashCode()).putExtra(Constants.option,Constants.history));
        StatisticsFragment.Statistics.updateReading(prefs,System.currentTimeMillis()-start_read_time);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(useVolumeKeys){
            switch (keyCode){
                case KeyEvent.KEYCODE_VOLUME_UP: if(reader.getLayoutDirection()!=View.LAYOUT_DIRECTION_RTL){prev();}else{next();} break;
                case KeyEvent.KEYCODE_VOLUME_DOWN: if(reader.getLayoutDirection()!=View.LAYOUT_DIRECTION_RTL){next();}else{prev();} break;
                default: super.onKeyDown(keyCode,event);
            }
        }
        return useVolumeKeys;
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        if((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)==0){
            statusBar.hide();
            root.setVisibility(View.VISIBLE);
        }else{
            statusBar.show();
            root.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_chapters,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home: finish(); break;
            case R.id.action_chapters: btn_next.setEnabled(num_chapter!=manga.getChapters().size()-1); rv.scrollToPosition(num_chapter); chaptersAdapter.notifyDataSetChanged(); Chapters.show(); break;
            case R.id.create_bookmark: manga.addBookMark(chapter,page); break;
            case R.id.change_reader_mode: new AlertDialog.Builder(this).setTitle(R.string.reader_mode)
                    .setSingleChoiceItems(R.array.reader_mode_entries, adapter.getReaderMode(), (dialog, i) -> {setModes(i, adapter.getScaleMode()); dialog.dismiss();})
                    .setNegativeButton(android.R.string.cancel, null).create().show(); break;
            case R.id.change_scale_mode: new AlertDialog.Builder(this).setTitle(R.string.scale_mode)
                    .setSingleChoiceItems(R.array.scale_mode_entries, adapter.getScaleMode(), (dialog, i) -> {setModes(adapter.getReaderMode(),i); dialog.dismiss();})
                    .setNegativeButton(android.R.string.cancel, null).create().show(); break;
            case R.id.share_page: Utils.Bitmap.shareBitmap(this,chapter.getName(),((BitmapDrawable)((ImageView)reader.getChildAt(reader.getChildCount()-1).findViewById(R.id.image)).getDrawable()).getBitmap());break;
            case R.id.share_screen: Utils.Bitmap.shareBitmap(this,chapter.getName(),Utils.Bitmap.screenView(reader));break;
            case R.id.settings: startActivity(new Intent(this, SettingsActivity.class).putExtra(SettingsActivity.KEY,SettingsActivity.TYPE_READER)); break;
            default: break;
        }
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(seekBar.getMax()>=0){
            progress_text.setText(getString(R.string.page)+" "+(progress+1)+"/"+(seekBar.getMax()+1));
        }else{
            progress_text.setText(R.string.loading_pages_list);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar){}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar){
        reader.scrollToPosition(seekBar.getProgress());
    }

    public void setBrightness(boolean system,int value){
        WindowManager.LayoutParams layout=getWindow().getAttributes();
        layout.screenBrightness=system ? lastBrightness : Math.min(Math.max(value/100f,0),1);
        getWindow().setAttributes(layout);
    }
    public float getBrightness(){return getWindow().getAttributes().screenBrightness;}
}