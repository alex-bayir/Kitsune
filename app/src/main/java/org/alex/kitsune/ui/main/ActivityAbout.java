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


public class ActivityAbout extends AppCompatActivity implements View.OnClickListener{
    Toolbar toolbar;
    TextView version,buildTime,progress;
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
}