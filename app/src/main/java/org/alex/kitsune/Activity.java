package org.alex.kitsune;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.Slide;
import android.transition.Transition;
import android.util.Pair;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import org.alex.kitsune.utils.Utils;

import java.util.Locale;

public abstract class Activity extends AppCompatActivity {
    private static boolean enable_animations=true;
    private SharedPreferences prefs;
    public final SharedPreferences getSharedPreferences(){
        return prefs;
    }
    public int getAnimationGravityIn(){
        return Gravity.NO_GRAVITY;
    }
    public int getAnimationGravityOut(){
        return Gravity.NO_GRAVITY;
    }
    private void init(){
        prefs=PreferenceManager.getDefaultSharedPreferences(this);
        enable_animations=prefs.getBoolean("animations",true);
        if(isEnableAnimations()){
            setAnimations(getAnimationGravityOut(),getAnimationGravityIn());
        }
    }
    private boolean isEnableAnimations(){
        return enable_animations=prefs.getBoolean("animations",true);
    }
    public static boolean isAnimationsEnable(){return enable_animations;}

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        init();
        setTheme(Utils.Theme.getTheme(this));
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==android.R.id.home){
            if(isEnableAnimations()){
                finishAfterTransition();
            }else{
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }


    public void setColorBars(){
        setColorBars(0,Utils.Theme.isThemeDark(this) ? 0 : getWindow().getStatusBarColor());
    }
    public void setColorBars(int statusBar,int navigationBar){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(statusBar);
        getWindow().setNavigationBarColor(navigationBar);
    }

    public void startActivity(Intent intent,int gravity_out,int gravity_in, Pair<View,String>... shared){
        startActivity(intent,animation(gravity_out,gravity_in,shared));
    }
    public void startActivity(Intent intent,Bundle bundle,int gravity_out,int gravity_in, Pair<View,String>... shared){
        bundle.putAll(animation(gravity_out,gravity_in,shared)); startActivity(intent,bundle);
    }
    public Bundle animation(int gravity_out,int gravity_in, Pair<View,String>... shared){
        return animation(this,gravity_out,gravity_in,shared);
    }
    public static Bundle animation(android.app.Activity activity, int gravity_out, int gravity_in, Pair<View,String>... shared){
        if(!enable_animations){return null;}
        if(gravity_out==Gravity.NO_GRAVITY || gravity_in==Gravity.NO_GRAVITY){return null;}
        setAnimations(activity,gravity_out,gravity_in);
        return ActivityOptions.makeSceneTransitionAnimation(activity,shared).toBundle();
    }
    public void setAnimations(int gravity_out, int gravity_in){
        setAnimations(this,gravity_out,gravity_in);
    }
    public static void setAnimations(android.app.Activity activity, int gravity_out, int gravity_in){
        Transition in=new Slide(gravity_in);
        Transition out=new Slide(gravity_out);
        Transition in_shared=new ChangeBounds();
        Transition out_shared=new ChangeBounds();
        in.setDuration(1000);
        out.setDuration(1000);
        in_shared.setDuration(in.getDuration());
        out_shared.setDuration(in_shared.getDuration());
        in.setInterpolator(new AccelerateDecelerateInterpolator());
        out.setInterpolator(new AccelerateDecelerateInterpolator());
        in_shared.setInterpolator(new AccelerateDecelerateInterpolator());
        out_shared.setInterpolator(new AccelerateDecelerateInterpolator());
        activity.getWindow().setExitTransition(out);
        activity.getWindow().setEnterTransition(in);
        if(System.currentTimeMillis()/1000%2==0){
            activity.getWindow().setReenterTransition(in);
            activity.getWindow().setReturnTransition(out);
        }
        activity.getWindow().setSharedElementExitTransition(out_shared);
        activity.getWindow().setSharedElementEnterTransition(in_shared);
    }

    public void setActivityFullScreen(){
        setActivityFullScreen(this);
    }
    public static void setActivityFullScreen(android.app.Activity activity){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P){
            activity.getWindow().getAttributes().layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        setVisibleSystemUI(activity,true);
    }
    public static void inverseVisibleSystemUI(android.app.Activity activity){
        setVisibleSystemUI(activity,!getVisibleSystemUI(activity));
    }
    public static boolean getVisibleSystemUI(android.app.Activity activity){
        View decorView=activity.getWindow().getDecorView();
        return ((decorView.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)==0);
    }
    public static void setVisibleSystemUI(android.app.Activity activity,boolean visible){
        View decorView=activity.getWindow().getDecorView();
        if(visible){
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }else{
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
    }
    @Deprecated
    public static void makeFullScreenActivity(android.app.Activity activity, boolean sticky){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            Window window=activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            //window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            //window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            //window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //int color = ContextCompat.getColor(activity, R.color.transparent_dark);
            //window.setStatusBarColor(color);
            //window.setNavigationBarColor(color);
        }
        View decorView=activity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | (sticky ? View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY : View.SYSTEM_UI_FLAG_IMMERSIVE) | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    public void restart() {
        restart(getIntent());
    }
    public void restart(Intent intent){
        if(isEnableAnimations()){
            finishAfterTransition();
        }else{
            finish();
        }
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }







    public static void clippingToolbarTexts(Toolbar toolbar, View.OnLongClickListener listener){
        for(int i=0; i<toolbar.getChildCount();i++){
            View view=toolbar.getChildAt(i);
            if(view instanceof TextView){
                view.setOnClickListener(v->{
                    Utils.setClipboard(v.getContext(),((TextView) v).getText().toString());
                    Toast.makeText(v.getContext(),((TextView) v).getText().toString(),Toast.LENGTH_SHORT).show();
                });
                view.setOnLongClickListener(listener);
            }
        }
    }
    public static void setLocale(String lang, Context context) {
        Configuration conf=context.getResources().getConfiguration();
        conf.setLocale(lang==null || "".equals(lang) ? null : new Locale(lang));
        context.getResources().updateConfiguration(conf,context.getResources().getDisplayMetrics());
    }
    public static void loadLocale(Context context){setLocale(PreferenceManager.getDefaultSharedPreferences(context).getString("language",null),context);}

    public static void callFilesStore(android.app.Activity activity, int requestCode, String types,int permission_request_code){
        if(ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
            activity.startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType(types), requestCode);
        }else{
            ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},permission_request_code);
        }
    }
}
