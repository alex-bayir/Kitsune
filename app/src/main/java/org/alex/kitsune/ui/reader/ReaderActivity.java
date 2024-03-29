package org.alex.kitsune.ui.reader;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.view.animation.LinearInterpolator;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import org.alex.kitsune.Activity;
import android.os.Bundle;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.commons.CustomSnackbar;
import org.alex.kitsune.commons.HttpStatusException;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.services.BookService;
import org.alex.kitsune.R;
import org.alex.kitsune.ui.shelf.StatisticsFragment;
import org.alex.kitsune.utils.Utils.Translator;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.ui.settings.SettingsActivity;
import org.alex.kitsune.book.BookMark;
import org.alex.kitsune.book.Chapter;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.ui.preview.CustomAdapter;
import org.alex.kitsune.commons.HolderListener;
import org.alex.kitsune.utils.*;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.alex.kitsune.ui.reader.ReaderPageHolder.ReaderMode;
import org.alex.kitsune.ui.reader.ReaderPageHolder.ScaleMode;

public class ReaderActivity extends Activity implements View.OnSystemUiVisibilityChangeListener,SeekBar.OnSeekBarChangeListener{

    ViewGroup root,frame;
    ReaderStatusBar statusBar;
    Toolbar toolBar;
    View bottomBar;
    SeekBar seekBar;
    TextView progress_text;
    RecyclerView reader;
    Dialog Chapters, FailedLoadPagesInfo;
    ReaderPageAdapter adapter;
    Book book;
    Chapter chapter;
    private int num_chapter;
    private int page=0;
    View menu_button,translate;
    Button btn_next;
    RecyclerView rv;
    CustomAdapter<Chapter> chaptersAdapter;
    boolean useVolumeKeys;
    float lastBrightness;
    Throwable lastThrowable;
    long start_read_time;
    TextView error_info;
    public CustomSnackbar getSnackbar(){
        return CustomSnackbar.makeSnackbar(frame,CustomSnackbar.LENGTH_INDEFINITE).setBackgroundAlpha(192).setIcon(R.drawable.ic_translate_yandex_transparent).setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL).setMargins(0,96,0,0);
    }

    private void setData(int num_chapter){
        chapter=book.getChapters().get(num_chapter);
        if(chapter!=null){
            toolBar.setTitle(book.getName());
            toolBar.setSubtitle(chapter.text(toolBar.getContext()));
            if(chapter.getPages()!=null && chapter.getPages().size()>0){
                seekBar.setMax(chapter.getPages().size()-1);
                adapter.setChapter(num_chapter);
                if(page==-1){page=chapter.getPages().size()-1;}
                reader.scrollToPosition(page);
                if(!getSharedPreferences().getBoolean("incognito",false)){
                    book.createHistory(chapter,page);
                }
                book.seen(num_chapter);
                seekBar.setProgress(page);
            }else{
                seekBar.setMax(-1);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull @NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(Constants.hash, book!=null ? book.hashCode():-1);
        outState.putBoolean(Constants.history,true);
    }

    @Override public int getAnimationGravityIn(){return Gravity.TOP;}
    @Override public int getAnimationGravityOut(){return Gravity.BOTTOM;}
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_reader);
        lastBrightness=getBrightness();
        frame=findViewById(R.id.frame);
        root=findViewById(R.id.root);
        Intent intent=getIntent();
        book=BookService.get(intent.getIntExtra(Constants.hash,savedState!=null ? savedState.getInt(Constants.hash,-1):-1));
        translate=findViewById(R.id.action_translate);
        translate.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN -> v.animate().scaleY(1.5f).scaleX(1.5f).setDuration(200).setInterpolator(new LinearInterpolator()).setListener(null).start();
                case MotionEvent.ACTION_UP -> v.animate().scaleY(1).scaleX(1).setDuration(200).setInterpolator(new LinearInterpolator()).setListener(null).start();
                case MotionEvent.ACTION_MOVE -> v.setPressed(false);
            }
            return false;
        });
        translate.setOnLongClickListener(v ->{
            List<ResolveInfo> list=Translator.getTranslators(ReaderActivity.this,resolveInfo -> resolveInfo);
            if(list.size()>1){
                PopupMenu popupMenu=new PopupMenu(ReaderActivity.this, translate,Gravity.END);
                for(ResolveInfo info:list){
                    popupMenu.getMenu().add(info.loadLabel(getPackageManager())).setIcon(info.loadIcon(getPackageManager())).setOnMenuItemClickListener(item -> {
                        new Thread(()->Translator.callTranslator(ReaderActivity.this,Utils.Bitmap.saveBitmap(Utils.Bitmap.screenView(reader), Bitmap.CompressFormat.JPEG,new File(getExternalCacheDir()+File.separator+"tmp.jpg")),info.activityInfo)).start(); return false;
                    });
                }
                popupMenu.setForceShowIcon(true);
                popupMenu.show();
            }else {
                new Thread(() -> Translator.callTranslator(ReaderActivity.this, Utils.Bitmap.saveBitmap(Utils.Bitmap.screenView(reader), Bitmap.CompressFormat.JPEG, new File(getExternalCacheDir() + File.separator + "tmp.jpg")), list.stream().map(resolveInfo -> resolveInfo.activityInfo).collect(Collectors.toList()))).start();
            }
            return true;
        });
        translate.setOnClickListener(v -> adapter.invertShowTranslate());
        reader=findViewById(R.id.reader);
        adapter=new ReaderPageAdapter(book,reader, v -> inverseVisibleSystemUI(this), v -> {if(reader.getLayoutDirection()!=View.LAYOUT_DIRECTION_RTL){prev();}else{next();}}, v -> {if(reader.getLayoutDirection()!=View.LAYOUT_DIRECTION_RTL){next();}else{prev();}});
        reader.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull @NotNull RecyclerView recyclerView, int newState) {
                seekBar.setProgress(page=adapter.getLayoutManager().findLastVisibleItemPosition());
            }
        });
        num_chapter=intent.getIntExtra(Constants.chapter,0);
        int num_bookMark=intent.getIntExtra(Constants.bookmark,-1);
        if(intent.getBooleanExtra(Constants.history,savedState!=null && savedState.getBoolean(Constants.history,false)) && book.getHistory()!=null){
            num_chapter=book.getNumChapterHistory();
            page=book.getHistory().getPage();
        }
        if(num_bookMark>=0 && book.getBookMarks()!=null && num_bookMark<book.getBookMarks().size()){
            final BookMark bookMark= book.getBookMarks().get(num_bookMark);
            num_chapter=book.getNumChapter(bookMark);
            page=bookMark.getPage();
            if(num_chapter<0 && NetworkUtils.isNetworkAvailable(this)){
                book.update((updated) -> {
                    if(updated){
                        book.updateDetails();
                        num_chapter= book.getNumChapter(bookMark.getChapter());
                        page=bookMark.getPage();
                        initChapter(num_chapter,page);
                    }
                },null);
            }
        }
        int color=ContextCompat.getColor(this,R.color.transparent_dark);
        setColorBars(color,color);
        setActivityFullScreen();
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);
        statusBar=findViewById(R.id.statusBar);
        statusBar.setIsActive(true);
        toolBar=findViewById(R.id.toolbar);
        initActionBar(toolBar);
        bottomBar=findViewById(R.id.bottomBar);
        seekBar=findViewById(R.id.progress);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setOnTouchListener((v, event) -> {if(event.getAction()==MotionEvent.ACTION_UP){page=Math.max(0,Math.min(seekBar.getMax(),seekBar.getProgress()));}return false;});
        progress_text=findViewById(R.id.progress_text);
        menu_button=findViewById(R.id.action_menu);
        menu_button.setOnClickListener(v -> {
            Drawable drawable=getDrawable(R.drawable.ic_checked);
            drawable.setState(new int[]{android.R.attr.state_checked});
            Drawable checked=drawable.getCurrent();

            PopupMenu popup=new PopupMenu(ReaderActivity.this, menu_button,Gravity.START);
            Menu menu=popup.getMenu();
            getMenuInflater().inflate(R.menu.options_reader,popup.getMenu());
            Arrays.stream(ReaderMode.values()).forEach(m->menu.findItem(m.id).setIcon(drawable));
            Arrays.stream(ScaleMode.values()).forEach(m->menu.findItem(m.id).setIcon(drawable));
            menu.findItem(ReaderMode.valueOf(adapter.getReaderMode()).id).setIcon(checked);
            menu.findItem(ScaleMode.valueOf(adapter.getScaleMode()).id).setIcon(checked);
            popup.setOnMenuItemClickListener(this::onOptionsItemSelected);
            popup.setForceShowIcon(true);
            popup.show();
        });
        {
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
            error_info=view.findViewById(R.id.info);
            FailedLoadPagesInfo=new AlertDialog.Builder(this).setView(view).create();
            FailedLoadPagesInfo.setOnShowListener(dialogInterface -> FailedLoadPagesInfo.findViewById(R.id.error).setVisibility(lastThrowable!=null ? View.VISIBLE : View.INVISIBLE));
            FailedLoadPagesInfo.setCancelable(false);
        }
        {
            View view=getLayoutInflater().inflate(R.layout.dialog_chapters,null);
            rv=view.findViewById(R.id.rv_list);
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.addItemDecoration(new DividerItemDecoration(rv.getContext(), DividerItemDecoration.VERTICAL));
            chaptersAdapter=new CustomAdapter<>(book,R.layout.item_chapter, new HolderListener() {
                @Override
                public void onItemClick(View v, int index){
                    initChapter(index,0);
                    Chapters.cancel();
                }
                @Override public boolean onItemLongClick(View v, int index){return false;}
            },null,rv,null);
            view.findViewById(R.id.close).setOnClickListener(v1 -> Chapters.cancel());
            btn_next=view.findViewById(R.id.next);
            btn_next.setOnClickListener(v2 -> {
                initChapter(num_chapter+1,0);
                Chapters.cancel();
            });
            Chapters=new AlertDialog.Builder(this).setView(view).create();
        }
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
        if(num_chapter<book.getChapters().size() && num_chapter>=0){
            this.num_chapter=num_chapter;
            this.page=page;
            if(book.checkChapterInfo(num_chapter)){
                setData(num_chapter);
            }else{
                if(NetworkUtils.isNetworkAvailable(this)){
                    lastThrowable=null;
                    new Thread(()->{
                        int pages=-1;
                        try{pages= book.getPages(num_chapter).size();}catch(Exception e){Logs.saveLog(lastThrowable=e);}
                        NetworkUtils.getMainHandler().post(pages>0? ()-> setData(num_chapter) : this::showErrorInfo);
                    }).start();
                }else{
                    showErrorInfo();
                    setData(num_chapter);
                }
            }
        }
    }

    private void showErrorInfo(){
        if(Utils.getRootCause(lastThrowable,3) instanceof HttpStatusException){
            error_info.setText("Page not found or not authorized");
        }else{
            error_info.setText("An a error has occurred");
        }
        FailedLoadPagesInfo.show();
    }
    public boolean setModes(ReaderMode R,ScaleMode S){
        return R!=null && S!=null && setModes(R.ordinal(), S.ordinal());
    }
    public boolean setModes(int modeR, int modeS){
        getSharedPreferences().edit().putInt(Constants.ReaderMode,modeR).putInt(Constants.ScaleMode,modeS).apply();
        adapter.setModes(modeR,modeS);
        seekBar.setLayoutDirection(reader.getLayoutDirection());
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs=getSharedPreferences();
        useVolumeKeys=prefs.getBoolean(Constants.use_volume_keys,true);
        setModes(prefs.getInt(Constants.ReaderMode,0),prefs.getInt(Constants.ScaleMode,0));
        frame.setBackgroundColor(prefs.getInt(Constants.custom_background_color,0));
        statusBar.setIsActive(prefs.getBoolean(Constants.show_status_bar,true));
        reader.setKeepScreenOn(prefs.getBoolean(Constants.keep_screen_on,false));
        setBrightness(!prefs.getBoolean(Constants.adjust_brightness,false),prefs.getInt(Constants.adjust_brightness_value,50));
        start_read_time=System.currentTimeMillis();
        adapter.notifyAllChanged();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!getSharedPreferences().getBoolean("incognito",false)){
            book.createHistory(chapter,page);
        }
        BookService.allocate(book,false);
        sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash, book.hashCode()).putExtra(Constants.option,Constants.history));
        StatisticsFragment.Statistics.updateReading(getSharedPreferences(),System.currentTimeMillis()-start_read_time);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(useVolumeKeys){
            switch (keyCode){
                case KeyEvent.KEYCODE_VOLUME_UP->{if(reader.getLayoutDirection()!=View.LAYOUT_DIRECTION_RTL){prev();}else{next();}}
                case KeyEvent.KEYCODE_VOLUME_DOWN->{if(reader.getLayoutDirection()!=View.LAYOUT_DIRECTION_RTL){next();}else{prev();}}
                default-> super.onKeyDown(keyCode,event);
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
    @SuppressLint("NotifyDataSetChanged")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.action_chapters) -> {btn_next.setEnabled(num_chapter!= book.getChapters().size()-1); rv.scrollToPosition(num_chapter); chaptersAdapter.notifyDataSetChanged(); Chapters.show();}
            case (R.id.create_bookmark) -> book.addBookMark(chapter,page);
            case (R.id.share_page) -> Utils.Bitmap.shareBitmap(this,chapter.getName(),((BitmapDrawable)((ImageView)reader.getChildAt(reader.getChildCount()-1).findViewById(R.id.image)).getDrawable()).getBitmap());
            case (R.id.share_screen) -> Utils.Bitmap.shareBitmap(this,chapter.getName(),Utils.Bitmap.screenView(reader));
            case (R.id.settings) -> startActivity(new Intent(this, SettingsActivity.class).putExtra(SettingsActivity.KEY,SettingsActivity.TYPE_READER),Gravity.START,Gravity.END);
            default -> {
                if( setModes(ReaderMode.fromId(item.getItemId()), ScaleMode.valueOf(adapter.getScaleMode())) | setModes(ReaderMode.valueOf(adapter.getScaleMode()), ScaleMode.fromId(item.getItemId())) ){
                    item.setChecked(true);
                }else{
                    super.onOptionsItemSelected(item);
                }
            }
        }
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(seekBar.getMax()>=0){
            progress_text.setText(getString(R.string.page,progress+1,seekBar.getMax()+1));
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