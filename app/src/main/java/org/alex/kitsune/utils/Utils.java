package org.alex.kitsune.utils;

import android.Manifest;
import android.app.ActivityManager;
import android.content.*;
import android.content.pm.*;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.HttpException;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip;
import org.alex.kitsune.BuildConfig;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.HttpStatusException;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class Utils {

    public static void registerAdapterDataChangeRunnable(RecyclerView.Adapter<? extends RecyclerView.ViewHolder> adapter, Runnable runnable){
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override public void onChanged(){runnable.run();}
            @Override public void onItemRangeRemoved(int positionStart, int itemCount){runnable.run();}
            @Override public void onItemRangeInserted(int positionStart, int itemCount){runnable.run();}
            @Override public void onStateRestorationPolicyChanged(){runnable.run();}
        });
    }
    public static class Translator{
        public static boolean callTranslator(Context context, java.io.File file){
            return file!=null && file.exists() && callAnyTranslator(context, Utils.File.toUri(context,file));
        }
        public static boolean callTranslator(Context context, java.io.File file, List<ResolveInfo> translators){
            return file!=null && file.exists() && callAnyTranslator(context, Utils.File.toUri(context,file),translators);
        }

        public static void callTranslator(Context context, java.io.File file, ActivityInfo activityInfo){
            context.startActivity(getIntent(Utils.File.toUri(context,file),activityInfo));
        }

        public static boolean callTranslator(Context context,Uri uri,String packageName,boolean callPlayMarket){
            PackageInfo packageInfo=Utils.App.getPackageInfo(context,packageName);
            if(packageInfo==null){
                if(callPlayMarket){Utils.App.callPlayMarkerToInstall(context,packageName);}
                return false;
            }
            for(ResolveInfo resolveInfo:context.getPackageManager().queryIntentActivities(getTranslatorsIntent(uri),0)){
                if(resolveInfo.activityInfo.packageName.equals(packageInfo.packageName)){
                    context.startActivity(getIntent(uri,resolveInfo.activityInfo.packageName,resolveInfo.activityInfo.name));
                    return true;
                }
            }
            return false;
        }
        public static Intent getTranslatorsIntent(Uri uri){return new Intent(Intent.ACTION_SEND).setType("image/*").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);}
        public static Intent getIntent(Uri uri, String packageName, String activityName){return new Intent(Intent.ACTION_SEND).setType("image/*").putExtra(Intent.EXTRA_STREAM,uri).setComponent(new ComponentName(packageName,activityName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);}
        public static Intent getIntent(Uri uri, ActivityInfo activityInfo){return getIntent(uri, activityInfo.packageName, activityInfo.name);}
        public static List<ResolveInfo> getTranslators(Context context){
            return getTranslators(context,Intent.ACTION_SEND,"image/*");
        }
        public static List<ResolveInfo> getTranslators(Context context,String action,String type){
            return context.getPackageManager().queryIntentActivities(new Intent(action).setType(type),0)
                    .stream().filter(resolveInfo -> resolveInfo.activityInfo.packageName.contains("translat"))
                    .collect(Collectors.toList());
        }
        public static List<ResolveInfo> getTextTranslators(Context context){
            return getTranslators(context,Intent.ACTION_PROCESS_TEXT,"text/plain");
        }
        public static boolean callAnyTranslator(Context context,Uri uri){
            return callAnyTranslator(context,uri,getTranslators(context));
        }
        public static boolean callAnyTranslator(Context context, Uri uri, List<ResolveInfo> translators){
            if(!callTranslator(context,uri,"ru.yandex.translate",translators.size()==0)){
                if(translators.size()>0){
                    context.startActivity(getIntent(uri,translators.iterator().next().activityInfo));
                    return true;
                }
            }
            return translators.size()>0;
        }
    }
    public static class App{
        public static void callPermissionsScreen(Context context,String settings){
            context.startActivity(new Intent(settings,Uri.parse("package:"+context.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        public static boolean checkInstalled(Context context, String packageName){return getPackageInfo(context,packageName)!=null;}
        public static void callPlayMarkerToInstall(Context context,String packageName){
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
        public static PackageInfo getPackageInfo(Context context, String packageName){
            try {
                return context.getApplicationContext().getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            }catch (PackageManager.NameNotFoundException e){
                return null;
            }
        }

    }
    public static class Activity {
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
        public static void callFilesStore(android.app.Activity activity, int requestCode, String types,int permission_request_code){
            if(ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
                activity.startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType(types), requestCode);
            }else{
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},permission_request_code);
            }
        }
        public static void setActivityFullScreen(android.app.Activity activity){
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P){
                activity.getWindow().getAttributes().layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            setVisibleSystemUI(activity,true);
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

        public static boolean getVisibleSystemUI(android.app.Activity activity){
            View decorView=activity.getWindow().getDecorView();
            return ((decorView.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)==0);
        }
        public static void inverseVisibleSystemUI(android.app.Activity activity){
            setVisibleSystemUI(activity,!getVisibleSystemUI(activity));
        }
        public static void setColorBars(android.app.Activity activity,int color_statusBar,int color_navigationBar){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                activity.getWindow().setStatusBarColor(color_statusBar);
                activity.getWindow().setNavigationBarColor(color_navigationBar);
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
        public static Context setLocale(String lang,Context context) {
            Configuration conf=context.getResources().getConfiguration();
            conf.setLocale(lang==null || "".equals(lang) ? null : new Locale(lang));
            context.getResources().updateConfiguration(conf,context.getResources().getDisplayMetrics());
            return context;
        }
        public static Context loadLocale(Context context){setLocale(PreferenceManager.getDefaultSharedPreferences(context).getString("language",null),context);return context;}
        public static void restartActivity(android.app.Activity activity) {
            restartActivity(activity,activity.getIntent());
        }
        public static void restartActivity(android.app.Activity activity, Intent intent){
            if(activity!=null){
                activity.finish();
                activity.startActivity(intent);
                activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        }
    }
    public static class File {
        public static String readFile(java.io.File file) throws FileNotFoundException {
            return readStream(new FileInputStream(file));
        }
        public static String readStream(InputStream in){
            Scanner scanner=new Scanner(in, "UTF-8").useDelimiter("\\A");
            String text=scanner.next();
            scanner.close();
            return text;
        }
        public static void writeFile(java.io.File file,String text,boolean append) throws FileNotFoundException {
            file.getParentFile().mkdirs();
            FileOutputStream stream=new FileOutputStream(file,append);
            try{
                stream.write(text.getBytes(StandardCharsets.UTF_8));
            }catch (IOException e){
                e.printStackTrace();
            } finally {
                try{stream.close();}catch(IOException e){e.printStackTrace();}
            }
        }
        public static String getFileName(Uri uri, ContentResolver contentResolver) {
            String result = null;
            if (uri.getScheme().equals("content")) {
                Cursor cursor = contentResolver.query(uri, null, null, null, null);
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    cursor.close();
                }
            }
            if (result == null) {
                result = uri.getPath();
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
            return result;
        }
        public static Uri toUri(Context context,java.io.File file){
            return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
        }
        public static boolean move(java.io.File from, java.io.File to){
            String tmp=from.getAbsolutePath();
            tmp=tmp.substring(0,tmp.lastIndexOf('/'));
            if(to.getAbsolutePath().startsWith(tmp)){
                return from.renameTo(to);
            }else{
                if(copy(from, to)){
                    delete(from);
                    return true;
                }
            }
            return false;
        }
        public static boolean copy(InputStream in,FileOutputStream out){
            try{
                byte[] buf=new byte[1024*1024];
                int read;
                while((read=in.read(buf))>0){out.write(buf,0,read);}
                in.close();
                out.close();
                return true;
            }catch(IOException e){
                e.printStackTrace();
                return false;
            }
        }
        public static boolean copy(java.io.File from,java.io.File to) {
            if(from.isDirectory()){
                boolean b=true;
                if(!to.exists() && !to.mkdirs()){return false;}
                java.io.File[] list=from.listFiles();
                if(list!=null){
                    for(java.io.File file:list){
                        b=b && copy(file,to);
                    }
                }
                return b;
            }else{
                try{
                    to.getParentFile().mkdirs();
                    return copy(new FileInputStream(from),new FileOutputStream(to));
                }catch(IOException e){
                    e.printStackTrace();
                    return false;
                }
            }
        }
        public static boolean delete(java.io.File file){
            if(file.isDirectory()){
                boolean b=true;
                java.io.File[] list=file.listFiles();
                if(list!=null){
                    for(java.io.File f:list){
                        b=b && delete(f);
                    }
                }
                return b && file.delete();
            }else{
                return file.delete();
            }
        }
        public static long getSize(java.io.File f){
            long size=0;
            if(f.isDirectory()){
                java.io.File[] list=f.listFiles();
                if(list!=null){for(java.io.File file:list){size+=getSize(file);}}
            }else{
                size=f.length();
            }
            return size;
        }
        private static final java.text.DecimalFormat f=new java.text.DecimalFormat("#.##");
        public static String SizeS(long size){
            double tmp=size; String c="bytes";
            if(size>1024){
                tmp/=1024; c="Kb";
                if(size>1024*1024){
                    tmp/=1024; c="Mb";
                    if(size>1024*1024*1024){
                        tmp/=1024; c="G";
                    }
                }
            }
            return f.format(tmp)+c;
        }

        public static void callPermissionManageStorage(android.app.Activity activity){
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.MANAGE_EXTERNAL_STORAGE}, 1);
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                activity.startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).setData(Uri.fromParts("package", activity.getPackageName(), null)));
            }
        }
    }
    public static class Menu{
        public static void buildIntentSubmenu(Context context, Intent intent, SubMenu subMenu) {
            PackageManager packageManager=context.getPackageManager();
            List<ResolveInfo> queryIntentActivities=packageManager.queryIntentActivities(intent, 0);
            subMenu.getItem().setVisible(!queryIntentActivities.isEmpty());
            for (ResolveInfo resolveInfo : queryIntentActivities){
                if(!context.getPackageName().equals(resolveInfo.activityInfo.packageName)){
                    subMenu.add(resolveInfo.loadLabel(packageManager)).setIcon(resolveInfo.loadIcon(packageManager)).setIntent(new Intent(intent).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED).setComponent(new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName, resolveInfo.activityInfo.name))).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
            }
        }
    }
    public static class Theme{
        private static final int[] APP_THEMES={R.style.AppTheme_DarkRed,R.style.AppTheme_DarkYellow,R.style.AppTheme_DarkGreen,R.style.AppTheme_LightRed,R.style.AppTheme_LightYellow,R.style.AppTheme_LightGreen,};
        public static int getTheme(int index){return APP_THEMES[Math.max(0,Math.min(APP_THEMES.length-1, index))];}
        public static int getTheme(SharedPreferences prefs){return getTheme(prefs.getInt("THEME",0));}
        public static int getTheme(Context context){return getTheme(PreferenceManager.getDefaultSharedPreferences(context));}
        public static boolean isThemeDark(Context context){
            switch (getTheme(context)){
                case R.style.AppTheme_DarkRed:
                case R.style.AppTheme_DarkYellow:
                case R.style.AppTheme_DarkGreen: return true;
                default: return false;
            }
        }
    }
    public static class Bitmap{
        public static IconCompat createShortcut(Context context,String iconPath){
            return IconCompat.createWithBitmap(Bitmap.createShortcutBitmap(context,iconPath));
        }
        public static android.graphics.Bitmap createShortcutBitmap(Context context,String iconPath){
            return createShortcutBitmap(context.getDrawable(R.drawable.ic_launcher_logo),BitmapFactory.decodeFile(iconPath),getLauncherIconSize(context));
        }
        public static android.graphics.Bitmap createShortcutBitmap(Drawable logo,android.graphics.Bitmap bitmap,int size){
            bitmap=ThumbnailUtils.extractThumbnail(bitmap,size,size);
            Canvas canvas=new Canvas(bitmap);
            logo.setBounds(canvas.getClipBounds());
            //logo.setAlpha(192);
            logo.draw(canvas);
            return bitmap;
        }
        public static android.graphics.Bitmap screenView(View v) {
            android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(v.getWidth(), v.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(bitmap);
            v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
            v.draw(canvas);
            return bitmap;
        }
        public static java.io.File saveBitmap(android.graphics.Bitmap bitmap,android.graphics.Bitmap.CompressFormat format,java.io.File file){
            if(bitmap!=null){
                try (FileOutputStream out=new FileOutputStream(file)) {
                    bitmap.compress(format, 100, out);
                    out.close();
                    return file;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            return null;
        }
        public static java.io.File saveBitmap(android.graphics.Bitmap bitmap,java.io.File file){return saveBitmap(bitmap,android.graphics.Bitmap.CompressFormat.PNG,file);}
        public static java.io.File saveBitmap(android.graphics.Bitmap bitmap, String fileName){return saveBitmap(bitmap,new java.io.File(fileName));}

        public static boolean shareBitmap(Context context,String title,android.graphics.Bitmap bitmap){
            if(bitmap!=null){
                context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("image/*").putExtra(Intent.EXTRA_TITLE,title).putExtra(Intent.EXTRA_STREAM, Utils.File.toUri(context,saveBitmap(bitmap, android.graphics.Bitmap.CompressFormat.JPEG,new java.io.File(context.getExternalCacheDir()+java.io.File.separator+"tmp.jpg")))),null));
            }
            return bitmap!=null;
        }
    }

    public static boolean createShortCutPreview(Context context,int hash,String name,String iconPath,Intent intent){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            /*
            ShortcutManager manager=ContextCompat.getSystemService(context,ShortcutManager.class);
            if(manager!=null && manager.isRequestPinShortcutSupported()){
                ShortcutInfo shortcut=new ShortcutInfo.Builder(context, context.getString(R.string.app_name)+" "+hash).setShortLabel(name).setIntent(intent).setIcon(Icon.createWithBitmap(Bitmap.createShortcutBitmap(context,iconPath))).build();
                return manager.requestPinShortcut(shortcut,PendingIntent.getBroadcast(context,0,manager.createShortcutResultIntent(shortcut),PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE).getIntentSender());
            }else{
                return false;
            }
             */
            return ShortcutManagerCompat.requestPinShortcut(context,new ShortcutInfoCompat.Builder(context, context.getString(R.string.app_name)+" "+hash).setShortLabel(name).setIntent(intent).setIcon(Bitmap.createShortcut(context,iconPath)).build(),null);
        }else{
            context.getApplicationContext().sendBroadcast(new Intent(Manifest.permission.INSTALL_SHORTCUT).putExtra(Intent.EXTRA_INTENT,intent).putExtra(Intent.EXTRA_SHORTCUT_NAME,name).putExtra(Intent.EXTRA_SHORTCUT_ICON, Bitmap.createShortcutBitmap(context,iconPath)));
        }
        return true;
    }


    public static boolean createShortCutPreview(android.app.Activity activity, int hash, String name, String iconPath, Intent intent){
        if(checkPermissions(activity,0,Manifest.permission.INSTALL_SHORTCUT)){
            return createShortCutPreview((Context) activity, hash, name, iconPath, intent);
        }else{
            return false;
        }
    }

    public static int getLauncherIconSize(Context context) {
        return Math.max(context.getSystemService(ActivityManager.class).getLauncherLargeIconSize(), (int) context.getResources().getDimension(android.R.dimen.app_icon_size));
    }

    public static float inRange(float min,float number,float max){return min>number ? min : max<number ? max : number;}

    public static void setClipboard(Context context, CharSequence text) {
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            ((android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("Copied Text", text));
        } else {
            ((android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE)).setText(text);
        }
    }

    public static boolean isServiceRunning(Context context,String serviceClassName){
        final ActivityManager activityManager = (ActivityManager) context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            android.util.Log.e(serviceClassName,runningServiceInfo.service.getClassName());
            if (runningServiceInfo.service.getClassName().equals(serviceClassName)){
                return true;
            }
        }
        return false;
    }

    public static long parseDate(String date,String format){
        try{
            return java.util.Objects.requireNonNull(new SimpleDateFormat(format,java.util.Locale.US).parse(date)).getTime();
        }catch (Exception e){
            return System.currentTimeMillis();
        }
    }

    public static boolean isUrl(String string){
        return string!=null && (string.startsWith("https://") || string.startsWith("http://"));
    }
    public static String unescape_unicodes(String escaped) {
        if(escaped==null){return null;}
        StringBuilder unescaped=new StringBuilder(escaped.length());
        char[] buffer=new char[6]; int len=0;
        int digits=0; boolean parse=false;
        for(int i=0;i<escaped.length();i++){
            char c=escaped.charAt(i);
            if(c=='\\' || c=='u'){
                if(len==1 && (buffer[0]!='\\' || c!='u')){
                    unescaped.append(buffer[--len]);  //len=0 - clear (rewrite) buffer
                }
                buffer[len++]=c; parse=true;
            }else if(parse){
                buffer[len++]=c;
                if(('0'<=c && c<='9') || ('a'<=c && c<='f') || ('A'<=c && c<='F')){
                    if(++digits==4){
                        int escape=0;
                        for(int j=len-digits;j<len;j++){
                            c=buffer[j];
                            escape=(escape<<4)+(c>='A' ? (c>='a'? c-'a'+10:c-'A'+10):c-'0');
                        }
                        unescaped.append((char) escape);
                        parse=false; digits=0; len=0; //len=0 - clear (rewrite) buffer
                    }
                }else{
                    unescaped.append(buffer,0,len);
                    parse=false; digits=0; len=0; //len=0 - clear (rewrite) buffer
                }
            }else{
                unescaped.append(c);
            }
        }
        return unescaped.toString();
    }
    public static String match(String text, String regex){
        StringBuilder builder=new StringBuilder();
        Matcher matcher=Pattern.compile(regex).matcher(text);
        while(matcher.find()){
            builder.append(text.substring(matcher.start(), matcher.end()));
        }
        return builder.toString();
    }
    public static String group(String text, String regex){
        StringBuilder builder=new StringBuilder();
        Matcher matcher=Pattern.compile(regex).matcher(text);
        while(matcher.find()){
            builder.append(matcher.group(1));
        }
        return builder.toString();
    }
    public static boolean checkPermissions(Context context, String... permissions){
        for (String permission : permissions){
            if(ContextCompat.checkSelfPermission(context, permission)!=PackageManager.PERMISSION_GRANTED){return false;}
        }
        return true;
    }
    public static boolean checkPermissions(android.app.Activity activity, int requestCode, String... permissions){
        boolean granted=checkPermissions(activity,permissions);
        if(!granted){
            ActivityCompat.requestPermissions(activity,permissions,requestCode);
        }
        return granted;
    }

    public static ArrayList<Integer> convert(Selection<Long> selection){return convert(selection,true);}
    public static ArrayList<Integer> convert(Selection<Long> selection, boolean sort){
        ArrayList<Integer> list=new ArrayList<>(selection.size());
        selection.forEach(l->list.add(l.intValue()));
        if(sort){list.sort(Integer::compareTo);}
        return list;
    }
    public static void showToolTip(View anchor,int text){
        showToolTip(anchor,text,R.layout.tooltip);
    }
    public static void showToolTip(View anchor,String text){
        showToolTip(anchor,text,R.layout.tooltip);
    }

    public static void showToolTip(View anchor, int text, View layout, int text_view_id){
        new SimpleTooltip.Builder(anchor.getContext()).anchorView(anchor).contentView(layout,text_view_id).text(text).arrowColor(anchor.getContext().getColor(R.color.transparent_dark)).gravity(Gravity.TOP).animated(false).transparentOverlay(false).build().show();
    }
    public static void showToolTip(View anchor,int text,int layout){
        new SimpleTooltip.Builder(anchor.getContext()).anchorView(anchor).contentView(layout).text(text).arrowColor(anchor.getContext().getColor(R.color.transparent_dark)).gravity(Gravity.TOP).animated(false).transparentOverlay(false).build().show();
    }
    public static void showToolTip(View anchor,String text,int layout){
        new SimpleTooltip.Builder(anchor.getContext()).anchorView(anchor).contentView(layout).text(text).arrowColor(anchor.getContext().getColor(R.color.transparent_dark)).gravity(Gravity.TOP).animated(false).transparentOverlay(false).build().show();
    }

    public static Throwable getRootCause(Throwable throwable, int depth){
        return change_known_errors(throwable!=null && throwable.getCause()!=null && (depth>0||depth==-1)? getRootCause(throwable.getCause(),--depth) : throwable);
    }
    public static Throwable getRootCause(GlideException throwable, int depth){
        List<Throwable> list;
        return getRootCause(throwable!=null && (list=throwable.getRootCauses()).size()>0 && (depth>0||depth==-1)? list.get(0) : throwable,depth);
    }
    public static Throwable change_known_errors(Throwable throwable){
        return throwable instanceof HttpException h? new HttpStatusException(h.getStatusCode(),null) : throwable;
    }

    public static void loadToView(View view,String url,String domain,Drawable error){
        Glide.with(view).load(NetworkUtils.getGlideUrl(url,domain)).error(error).into(new CustomViewTarget<View,Drawable>(view) {
            @Override
            public void onLoadFailed(@Nullable @org.jetbrains.annotations.Nullable Drawable errorDrawable) {
                getView().setBackground(errorDrawable);
            }
            @Override
            public void onResourceReady(@NonNull @NotNull Drawable resource, @Nullable @org.jetbrains.annotations.Nullable Transition<? super Drawable> transition) {
                getView().setBackgroundDrawable(resource);
            }
            @Override
            protected void onResourceCleared(@Nullable @org.jetbrains.annotations.Nullable Drawable placeholder) {}
        });
    }
}
