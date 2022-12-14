package org.alex.kitsune.ui.main.scripts;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.alex.edittextcode.EditTextCode;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import fr.castorflex.android.circularprogressbar.CircularProgressBar;
import fr.castorflex.android.circularprogressbar.CircularProgressDrawable;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.manga.Manga_Scripted;
import org.alex.kitsune.scripts.Script;
import org.alex.kitsune.manga.search.FilterSortAdapter;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.utils.LoadTask;
import org.alex.kitsune.utils.Utils;
import org.alex.kitsune.R;
import java.io.*;

public class CompilerActivity extends AppCompatActivity {

    View fab;
    File file;
    EditTextCode editor;
    TextView logcat;
    CircularProgressBar progressBar;
    BottomSheetBehavior<TextView> b;
    int mode=R.id.base_functions;
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        setTheme(Utils.Theme.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compiler);
        file=new File(getIntent().getStringExtra(Constants.file));
        fab=findViewById(R.id.fab);
        editor=findViewById(R.id.editor);
        logcat=findViewById(R.id.logcat);
        logcat.setHorizontallyScrolling(true);

        progressBar=findViewById(R.id.progress);
        progressBar.setIndeterminateDrawable(new CircularProgressDrawable.Builder(this).colors(new int[]{0xff0000ff,0xff00ffff,0xff00ff00,0xffffff00,0xffff0000,0xffff00ff}).style(CircularProgressDrawable.STYLE_ROUNDED).strokeWidth(8f).sweepInterpolator(new AccelerateDecelerateInterpolator()).build());
        progressBar.setVisibility(View.INVISIBLE);

        if(file.exists()){try{editor.setText(Utils.File.readFile(file)); Script script=Script.getInstance(file); editor.setSyntaxHighlightRules(script.getSyntaxHighlightRules()); editor.setSuggestionsSet(script.getSuggestionsSet());}catch(Throwable e){e.printStackTrace();}}
        b=BottomSheetBehavior.from(logcat);
        b.setState(BottomSheetBehavior.STATE_HIDDEN);
        fab.setOnClickListener(v->{
            editor.updateSyntax();
            b.setState(BottomSheetBehavior.STATE_HIDDEN);
            fab.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.startNestedScroll(0);
            new LoadTask<Void,Void,Throwable>(){
                final ByteArrayOutputStream out=new ByteArrayOutputStream();
                @Override protected Throwable doInBackground(Void unused) {
                    try {
                        Utils.File.writeFile(file,editor.getText().toString(),false);
                        Script script=Script.getInstance(file);
                        Manga_Scripted.getScripts().put(script.getName(),script);
                        script.setSTDOUT(new PrintStream(out));
                        script.setSTDERR(new PrintStream(out));
                        switch (mode){
                            default: f1(script); break;
                            case R.id.advanced_search_functions: f2(script); break;
                        }
                    }catch(Throwable e){return e;}
                    return null;
                }
                @Override protected void onFinished(Throwable throwable) {
                    String str=out.toString();
                    if(throwable!=null){str+=Logs.getStackTrace(throwable);}
                    logcat.setText(str);
                    b.setPeekHeight(Math.min(logcat.getHeight(),editor.getHeight()));
                    b.setState(BottomSheetBehavior.STATE_EXPANDED);
                    progressBar.progressiveStop();
                    progressBar.setVisibility(View.GONE);
                    fab.setEnabled(true);
                }
            }.start(null);
        });
    }

    private void f1(Script script) throws Exception {
        Manga manga=Manga_Scripted.query(script,"Tower",0).get(0);
        manga.update();
        manga.getPages(0);
    }
    private void f2(Script script) throws Exception{
        FilterSortAdapter adapter=Manga_Scripted.createAdvancedSearchAdapter(script);
        Manga_Scripted.query(script,"Tower",0,(Object[]) adapter.getOptions());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_compiler,menu);
        menu.findItem(R.id.base_functions).setChecked(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); break;
            case R.id.action_open_api: startActivity(new Intent(this,ApiActivity.class)); break;
            case R.id.base_functions:
            case R.id.advanced_search_functions: item.setChecked(true); mode=item.getItemId();break;
        }
        return super.onOptionsItemSelected(item);
    }
}