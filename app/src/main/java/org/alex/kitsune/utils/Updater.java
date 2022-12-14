package org.alex.kitsune.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;
import androidx.preference.PreferenceManager;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.commons.CustomSnackbar;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.manga.Manga_Scripted;
import org.alex.kitsune.scripts.Script;
import org.alex.kitsune.ui.main.Constants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.alex.kitsune.BuildConfig;

public class Updater {
    private static JSONObject updateInfo=null;
    private static File f;
    public static void init(Context context){
        f=new File(context.getExternalCacheDir().getAbsolutePath()+File.separator+"update.apk"); f.delete();
        if(compareVersions(PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.version,""),BuildConfig.VERSION_NAME)!=0){
            showWhatisNew(context,true);
            Logs.clearAll();
        }
    }
    private static JSONObject getUpdate(Context context){
        if(NetworkUtils.isNetworkAvailable(context)){
            try{
                JSONArray json=new JSONArray(NetworkUtils.getString("https://api.github.com/repos/alex-bayir/Kitsune/releases"));
                for(int i=0;i<json.length();i++){
                    JSONObject jo=json.getJSONObject(i);
                    String version=Utils.match(jo.getString("tag_name"),"\\d.*\\d"),url=null;
                    JSONArray assets=jo.optJSONArray("assets");
                    for(int j=0;j<(assets!=null?assets.length():0) && (url==null || !url.contains(".apk"));j++){
                        url=assets.getJSONObject(j).optString("browser_download_url");
                    }
                    if(url!=null && url.contains(".apk")){
                        switch (compareVersions(BuildConfig.VERSION_NAME,version)){
                            case 0: if(!BuildConfig.BUILD_TYPE.equals("debug")){break;}
                            case 1: return new JSONObject().put("url",url).put("version",version);
                            case -1: break;
                        }
                    }
                }
            }catch (Exception e){
                Logs.saveLog(e);
            }
        }
        return null;
    }
    private static boolean updateScripts(Context context){
        AtomicBoolean update=new AtomicBoolean(false);
        if(NetworkUtils.isNetworkAvailable(context)){
            String dir=context.getExternalFilesDir(Constants.manga_scripts).getAbsolutePath();
            LinkedHashSet<String> set=new LinkedHashSet<>();
            SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(context);
            Set<String> hashes=prefs.getStringSet(Constants.scripts_hashes,new HashSet<>());
            try{
                JSONArray json=new JSONArray(NetworkUtils.getString("https://api.github.com/repos/alex-bayir/Kitsune/contents/app/src/main/assets/scripts"));
                for(int i=0;i<json.length();i++) {
                    int index=i;
                    Thread thread=new Thread(() -> {
                        try{
                            JSONObject jo=json.getJSONObject(index);
                            if(!hashes.contains(jo.getString("sha"))){
                                String url=jo.getString("download_url");
                                String text_script=NetworkUtils.getString(url);
                                Script script=Manga_Scripted.getScript(Utils.group(url,".*/(.*).lua"));
                                if(text_script!=null){
                                    Utils.File.writeFile(new File(script!=null?script.getPath():dir+Utils.group(url,".*/(.*)")),text_script,false);
                                    update.set(true);
                                }
                            }
                            set.add(jo.getString("sha"));
                        }catch (IOException | JSONException e){
                            Logs.saveLog(e);
                        }
                    });
                    thread.start();
                    try{thread.join();}catch(InterruptedException ignored){}
                }
                if(set.size()>0){
                    prefs.edit().putStringSet(Constants.scripts_hashes,set).apply();
                }
            }catch (IOException | JSONException e){
                Logs.saveLog(e);
            }
        }
        return update.get();
    }

    public static void checkAndUpdateScripts(Activity activity,Callback<Boolean> callback){
        if(PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("update_scripts_automatically",true)){
            if(NetworkUtils.isNetworkAvailable(activity)){
                if(callback!=null){callback.call(true);}
                new LoadTask<Activity,Void,Boolean>(){
                    @Override
                    protected Boolean doInBackground(Activity activity) {
                        return updateScripts(activity);
                    }
                    @Override
                    protected void onFinished(Boolean update) {
                        if(update){Utils.Activity.restartActivity(activity);}
                        Toast.makeText(activity,R.string.all_scripts_updated,Toast.LENGTH_LONG).show();
                        if(callback!=null){callback.call(false);}
                    }
                    @Override
                    protected void onBraked(Throwable throwable) {
                        if(callback!=null){callback.call(false);}
                    }
                }.start(activity);
            }else{
                Toast.makeText(activity,R.string.no_internet,Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static String getUrl(){return updateInfo!=null ? updateInfo.optString("url",null) : null;}
    public static String getVersion(){return updateInfo!=null ? updateInfo.optString("url",null) : null;}
    public static File getFile(){return f;}
    public static void getUpdate(Context context,Callback<JSONObject> callback){
        if(updateInfo!=null){callback.call(updateInfo);}else{
            if(NetworkUtils.isNetworkAvailable(context)){
                new Thread(()->{
                    updateInfo=getUpdate(context); new Handler(Looper.getMainLooper()).post(()-> {if(callback!=null){callback.call(updateInfo);}});
                }).start();
            }
        }
    }
    public static void loadUpdate(Callback<String> progressUpdate, Callback<Boolean> finished){
        if(!f.exists()){
            String url=updateInfo!=null ? updateInfo.optString("url",null) :null;
            if(url!=null){
                new LoadTask<String,String,Boolean>(){
                    @Override protected Boolean doInBackground(String s){return loadInBackground(s,null,f,null,getHandler(),false);}
                    @Override protected void onProgressUpdate(String s){if(progressUpdate!=null){progressUpdate.call(s);}}
                    @Override protected void onFinished(Boolean b){if(finished!=null){finished.call(b);}}
                }.start(url);
            }
        }else{
            if(finished!=null){finished.call(true);}
        }
    }
    public static void installUpdate(Context context){
        context.startActivity(new Intent(Intent.ACTION_VIEW).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION).setDataAndType(Utils.File.toUri(context,f),"application/vnd.android.package-archive"));
    }
    public static int getStatusIcon(){
        return f.exists() ? R.drawable.ic_update : (getUrl()!=null ? R.drawable.ic_download_arrow : R.drawable.ic_search);
    }
    public static Drawable getStatusIcon(Context context){
        return context.getDrawable(getStatusIcon());
    }

    public static int compareVersions(String v1, String v2){
        if(v1==null || v2==null || v1.length()==0 || v2.length()==0){return -1;}
        String[] v1s=v1.split("\\."), v2s=v2.split("\\.");
        for(int i=0;i<v1s.length && i<v2s.length;i++){
            if(!v1s[i].equals(v2s[i])){
                return Integer.parseInt(v1s[i])<Integer.parseInt(v2s[i]) ? 1:-1;
            }
        }
        return v1s.length<v2s.length ? 1:0;
    }

    public static CustomSnackbar createSnackBarUpdate(ViewGroup parent, int gravity, int duration, String text, View.OnClickListener update){
        return CustomSnackbar.makeSnackbar(parent, duration).setGravity(gravity).setText(text).setIcon(R.drawable.ic_caution_yellow).setAction(R.string.update,update).setBackgroundAlpha(200);
    }
    public static CustomSnackbar createSnackBarUpdate(ViewGroup parent, int gravity, int duration, View.OnClickListener update){
        return createSnackBarUpdate(parent,gravity,duration,parent.getContext().getString(R.string.new_version_founded)+" "+updateInfo.optString("version"),update);
    }
    public static Dialog createDialogUpdate(Context context, View.OnClickListener update){
        Dialog dialog=new Dialog(context);
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        View view=LayoutInflater.from(context).inflate(R.layout.dialog_update, null,false);
        dialog.setContentView(view);
        ((TextView)view.findViewById(R.id.title)).setText(context.getString(R.string.new_version_founded));
        view.findViewById(R.id.cancel).setOnClickListener(v->dialog.dismiss());
        view.findViewById(R.id.update).setOnClickListener(v->{update.onClick(v); dialog.dismiss();});
        return dialog;
    }

    public static void showWhatisNew(Context context,boolean all){
        Dialog dialog=new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_whatnew);
        if(all){
            ((TextView)dialog.findViewById(R.id.text)).setHorizontallyScrolling(true);
            ((TextView)dialog.findViewById(R.id.text)).setMovementMethod(new ScrollingMovementMethod());
        }else{
            String tmp=context.getString(R.string.what_new_innovations);
            ((TextView)dialog.findViewById(R.id.text)).setText(tmp.substring(0,tmp.indexOf("\n\n")));
        }
        dialog.show();
    }
}
