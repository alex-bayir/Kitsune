package org.alex.kitsune.ui.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import com.alex.shimmer.Shimmer;
import com.alex.shimmer.ShimmerTextView;
import com.alex.json.java.JSON;
import org.alex.kitsune.BuildConfig;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.AspectRatioImageView;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Updater;
import org.alex.kitsune.utils.Utils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Comparator;


public class ActivityAbout extends AppCompatActivity implements View.OnClickListener{
    Toolbar toolbar;
    TextView version,buildTime,progress,downloads;
    ImageView launcher,update;
    SharedPreferences prefs;
    Callback<JSON.Object> ucs= json -> {
        update.setImageDrawable(Updater.getStatusIcon(this));
        update.setEnabled(json!=null);
        if(json==null){
            update.setVisibility(View.GONE); Toast.makeText(this,R.string.no_updates_found,Toast.LENGTH_SHORT).show();
        }
    };
    Callback<JSON.Object> ucl=json -> Updater.loadUpdate(p->{
        if(progress.getVisibility()!=View.VISIBLE){progress.setVisibility(View.VISIBLE);}
        update.setEnabled(false);
        progress.setText(p);
    },b->{
        progress.setVisibility(View.GONE);
        update.setEnabled(true);
        update.setImageDrawable(Updater.getStatusIcon(this));
        if(b){
            Updater.installUpdate(this);
        }
    });
    private final String card=BuildConfig.card;
    private static JSON.Object json=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.Theme.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        prefs=PreferenceManager.getDefaultSharedPreferences(this);
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        toolbar.setTitle(R.string.About);

        Shimmer shimmer=new Shimmer().setDuration(2000);
        version=findViewById(R.id.version);
        version.setText(getString(R.string.version,BuildConfig.VERSION_NAME,BuildConfig.VERSION_CODE));
        buildTime=findViewById(R.id.time);
        buildTime.setText(getString(R.string.build_time,new SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.getDefault()).format(new Date(BuildConfig.TIMESTAMP)),BuildConfig.BUILD_TYPE));
        launcher=findViewById(R.id.launcher_icon);
        launcher.setOnClickListener(this);
        init(shimmer,findViewById(R.id.profile_in_vk));
        init(shimmer,findViewById(R.id.source_code));
        init(shimmer,findViewById(R.id.play_market));
        init(shimmer,findViewById(R.id.card)).setText(getString(R.string.donate,card));
        if(card.length()==0){((View)findViewById(R.id.card).getParent()).setVisibility(View.GONE);}
        update=findViewById(R.id.update);
        update.setOnClickListener(this);
        update.setImageDrawable(Updater.getStatusIcon(this));
        progress=findViewById(R.id.progress);
        progress.setVisibility(View.GONE);
        if(getIntent().getBooleanExtra("update",false)){
            update.performClick();
        }
        downloads=findViewById(R.id.downloads);
        if(json!=null && json.size()>0){
            downloads.setText(format_downloads(json));
        }else{
            new Thread(()->{
                try {json=count_downloads(JSON.Object.create(prefs.getString("downloads","")));} catch (Throwable e) {e.printStackTrace();}
                if(json!=null){
                    NetworkUtils.getMainHandler().post(()->{
                        prefs.edit().putString("downloads",json.toString()).apply();
                        downloads.setText(format_downloads(json));
                    });
                }else{
                    NetworkUtils.getMainHandler().post(()->{
                        try{
                            downloads.setText(format_downloads(JSON.Object.create(prefs.getString("downloads",""))));
                        }catch (Exception e) {
                            ((View)downloads.getParent()).setVisibility(View.GONE);
                            e.printStackTrace();
                        }
                    });
                }
            }).start();
        }
    }
    private ShimmerTextView init(Shimmer shimmer,ShimmerTextView v){v.setOnClickListener(this); shimmer.start(v); return v;}

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home->finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case (R.id.launcher_icon) -> new AlertDialog.Builder(v.getContext()).setView(new AspectRatioImageView(v.getContext(), launcher.getScaleType(), launcher.getDrawable())).create().show();
            case (R.id.profile_in_vk) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://vk.com/org.alex.kitsune")));
            case (R.id.source_code) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/alex-bayir/Kitsune")));
            case (R.id.play_market) -> startActivity(Utils.App.getIntentOfCallPlayMarketToInstall(getPackageName()));
            case (R.id.card) -> {Utils.setClipboard(this, card);Toast.makeText(v.getContext(), card, Toast.LENGTH_SHORT).show();}
            case (R.id.update) -> {
                if (NetworkUtils.isNetworkAvailable(this)) {
                    update.setEnabled(false);
                    Updater.getUpdate(this, Updater.getUrl() == null ? ucs : ucl);
                } else {
                    Toast.makeText(this, R.string.no_internet, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public JSON.Object count_downloads(JSON.Object def) throws Throwable {
        JSON.Object obj=new JSON.Object(def);
        JSON.Array<?> json=NetworkUtils.getJSON("https://api.github.com/repos/alex-bayir/Kitsune/releases").array();
        for(int i=0;i<json.size();i++){
            JSON.Object jo=json.getObject(i);
            String version=Utils.match(jo.getString("tag_name"),"\\d.*\\d"),url=null;
            JSON.Array<?> assets=jo.getArray("assets");
            for(int j=0;j<(assets!=null?assets.size():0) && (url==null || !url.contains(".apk"));j++){
                url=assets.getObject(j).getString("browser_download_url");
            }
            if(url!=null && url.contains(".apk")){
                JSON.Object asset=jo.getArray("assets").getObject(0);
                String date=asset.getString("updated_at").replaceAll("[TZ]"," ");
                long timestamp=Utils.parseDate(date,"yyyy-MM-dd HH:mm:ss");
                obj.put(Long.toString(timestamp),new JSON.Object().put("version",version).put("date",date).put("count",asset.getInt("download_count")));
            }
        }
        return obj;
    }
    public String format_downloads(JSON.Object array) {
        StringBuilder builder = new StringBuilder();
        String format = "%9.9s | %-19.19s | %-9.9s\n";
        builder.append(String.format(Locale.getDefault(), format, "Downloads", "       Date        ", "Version"));
        builder.append("----------+---------------------+----------\n");
        int len = builder.length();
        array.forEach((key, value) -> {
            JSON.Object json = (JSON.Object) value;
            builder.insert(len, String.format(Locale.getDefault(), format, json.getInt("count"), json.getString("date"), json.getString("version")));
        });
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }
}