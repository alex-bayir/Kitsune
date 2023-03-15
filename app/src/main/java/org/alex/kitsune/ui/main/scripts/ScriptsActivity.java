package org.alex.kitsune.ui.main.scripts;

import android.content.Intent;
import android.os.FileUtils;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.AppBarStateChangeListener;
import org.alex.kitsune.commons.CustomSnackbar;
import org.alex.kitsune.manga.Manga_Scripted;
import org.alex.kitsune.scripts.Script;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.commons.HolderListener;
import org.alex.kitsune.ui.shelf.Catalogs;
import org.alex.kitsune.utils.Utils;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class ScriptsActivity extends AppCompatActivity {
    Toolbar toolbar;
    RecyclerView rv;
    ScriptsAdapter adapter;
    TextView noScripts;
    View fab;
    File dir;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.Theme.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scripts);
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        Utils.Activity.setColorBars(this,0,0);
        Utils.Activity.setActivityFullScreen(this);
        AppBarLayout barLayout=findViewById(R.id.app_bar);
        barLayout.addOnOffsetChangedListener(new AppBarStateChangeListener() {
            @Override
            public void onStateChanged(AppBarLayout appBarLayout, State state) {
                if(state!=State.IDLE){
                    Utils.Activity.setVisibleSystemUI(ScriptsActivity.this,state==State.COLLAPSED);
                }
            }
        });
        toolbar.setTitle("Scripts");
        noScripts=findViewById(R.id.text);
        noScripts.setText(R.string.no_scripts);
        rv=findViewById(R.id.rv_list);

        adapter=new ScriptsAdapter(dir=getExternalFilesDir(Constants.manga_scripts), new View.OnFocusChangeListener() {
            File last;
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(v.getId()==R.id.name){
                    if(hasFocus){last=getFileByName(adapter.getFiles(),((TextView)v).getText().toString());}
                    else{
                        String newname=((TextView)v).getText().toString();
                        if(last!=null && Script.checkSuffix(newname) && getFileByName(adapter.getFiles(),newname)==null){
                            adapter.update(last,new File(dir.getAbsolutePath()+"/"+newname));
                            Manga_Scripted.setScripts(Catalogs.getMangaScripts(getExternalFilesDir(Constants.manga_scripts)));
                        }
                        last=null;
                    }
                }
            }
        }, new HolderListener() {
            @Override
            public void onItemClick(View v, int index) {
                startActivity(new Intent(ScriptsActivity.this,CompilerActivity.class).putExtra(Constants.file,adapter.getFiles().get(index).getAbsolutePath()));
            }

            @Override
            public boolean onItemLongClick(View v, int index) {
                return false;
            }
        }, (position, item) -> {
            switch (item.getItemId()) {
                case ScriptHolder.REMOVE:
                    adapter.delete(position);
                    Manga_Scripted.setScripts(Catalogs.getMangaScripts(getExternalFilesDir(Constants.manga_scripts)));
                    break;
            }
            return false;
        });
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(this));
        Utils.registerAdapterDataChangeRunnable(adapter,()->noScripts.setVisibility(adapter.getItemCount()==0?View.VISIBLE:View.GONE));
        fab=findViewById(R.id.fab);
        fab.setOnClickListener(v -> Utils.Activity.callFilesStore(ScriptsActivity.this,1,"text/plain",0));
        CustomSnackbar.makeSnackbar((ViewGroup) fab.getParent(), Snackbar.LENGTH_LONG).setText(R.string.scripts_updates).setIcon(R.drawable.ic_caution_yellow).setGravity(Gravity.CENTER).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.update();
        rv.clearFocus();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scripts,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home: finish(); break;
            case R.id.action_create_script: startActivity(new Intent(ScriptsActivity.this,CompilerActivity.class).putExtra(Constants.file,dir.getAbsolutePath()+"/newScript"+System.currentTimeMillis()+".py")); break;
            case R.id.action_open_api: startActivity(new Intent(this,ApiActivity.class)); break;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        switch(requestCode){
            case 0: break;
            case 1:
                if(resultCode==RESULT_OK){
                    try{
                        FileUtils.copy(getContentResolver().openInputStream(data.getData()),new FileOutputStream(dir.getPath()+"/"+Utils.File.getFileName(data.getData(),getContentResolver())));
                        adapter.update();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    public static File getFileByName(ArrayList<File> files, String name){for(File file:files){if(file.getName().equals(name)){return file;}} return null;}
}