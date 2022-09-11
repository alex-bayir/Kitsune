package org.alex.kitsune.services;

import android.app.*;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import android.view.WindowManager.LayoutParams;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import org.alex.kitsune.ui.main.MainActivity;
import org.alex.kitsune.R;
import org.alex.kitsune.utils.Utils.Translator;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.utils.Utils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import static android.app.Activity.RESULT_OK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class TranslateService extends Service implements ImageReader.OnImageAvailableListener{
    private WindowManager windowManager;
    private ImageView root;
    //private TextView text;
    private MediaProjection projection=null;
    private VirtualDisplay virtualDisplay=null;
    private ImageReader reader=null;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private NotificationManagerCompat notificationManagerCompat;
    private final String channelID="Open translator";
    @Override
    public void onCreate() {
        setTheme(Utils.Theme.getTheme(this));
        super.onCreate();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)){
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())).setFlags(FLAG_ACTIVITY_NEW_TASK));
        }
        notificationManagerCompat=NotificationManagerCompat.from(this);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            notificationManagerCompat.createNotificationChannel(new NotificationChannel(channelID,channelID, NotificationManager.IMPORTANCE_HIGH));
        }
        root=new ImageView(this);
        root.setImageResource(R.drawable.ic_translate_yandex);
        root.setClickable(true);
        //text=new TextView(this);
        //text.setText("Check notification");
        //text.setVisibility(View.GONE);
        //text.setTextColor(0xffff0000);
        windowManager=(WindowManager) getSystemService(WINDOW_SERVICE);
        final LayoutParams layoutParams=new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O ? LayoutParams.TYPE_APPLICATION_OVERLAY : LayoutParams.TYPE_PHONE), LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        windowManager.addView(root,layoutParams);
        //windowManager.addView(text,new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O ? LayoutParams.TYPE_APPLICATION_OVERLAY : LayoutParams.TYPE_PHONE), LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT));
        root.setOnTouchListener(new View.OnTouchListener() {
            private int x;
            private int y;
            private float xT;
            private float yT;
            private long downTime;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        downTime=System.currentTimeMillis();
                        x = layoutParams.x;
                        y = layoutParams.y;
                        xT = event.getRawX();
                        yT = event.getRawY();
                        v.animate().scaleY(1.5f).scaleX(1.5f).setDuration(200).setInterpolator(new LinearInterpolator()).setListener(null).start();
                        break;
                    case MotionEvent.ACTION_UP:
                        v.animate().scaleY(1).scaleX(1).setDuration(200).setInterpolator(new LinearInterpolator()).setListener(null).start();
                        if(System.currentTimeMillis()-downTime<ViewConfiguration.getLongPressTimeout()){
                            //sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
                            startService(new Intent(TranslateService.this,TranslateService.class).setAction("start"));
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        v.setPressed(false);
                        layoutParams.x = x + (int) (event.getRawX() - xT);
                        layoutParams.y = y + (int) (event.getRawY() - yT);
                        windowManager.updateViewLayout(v, layoutParams);
                        break;
                }
                return false;
            }
        });

    }

    public Notification sendNotification(File file){
        PendingIntent pendingIntent=PendingIntent.getActivity(this,0,Translator.getTranslatorsIntent(Utils.File.toUri(file)),PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification=new NotificationCompat.Builder(this,channelID)
                .setSmallIcon(R.drawable.ic_launcher_logo)
                .setContentTitle("Open translator")
                .setContentText("Screenshot is ready")
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true).build();
        notificationManagerCompat.notify(10023,notification);
        return notification;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null){
            if("start".equals(intent.getAction())){
                handler.postDelayed(this::makeScreenShot, 100);
            }else{
                Intent data=intent.getParcelableExtra("data");
                if(data==null){
                    Toast.makeText(this,"key \"data\" is null\n if you developer put to intent result of MediaProjectionManager",Toast.LENGTH_LONG).show();
                    stopSelf();
                }else{
                    projection=MainActivity.projectionManager.getMediaProjection(RESULT_OK,data);
                }
            }
        }
        return START_STICKY;
    }
    public void makeScreenShot(){
        notificationManagerCompat.cancel(10023);
        if(virtualDisplay==null && projection!=null){
            DisplayMetrics d=getResources().getDisplayMetrics();
            reader=ImageReader.newInstance(d.widthPixels,d.heightPixels,ImageFormat.FLEX_RGBA_8888,2);
            reader.setOnImageAvailableListener(this,handler);
            virtualDisplay=projection.createVirtualDisplay("ScreenCapture",d.widthPixels,d.heightPixels,d.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,reader.getSurface(),null,handler);
            root.setVisibility(View.INVISIBLE);
        }
    }

    public void openStatusBar(){
        try{
            StatusBarManager service=getSystemService(StatusBarManager.class);
            Method expand;
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
                expand=StatusBarManager.class.getMethod("expandNotificationsPanel");
            }else{
                expand=StatusBarManager.class.getMethod("expand");
            }
            expand.invoke(service);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(reader!=null){
            reader.setOnImageAvailableListener(null,null);
            reader=null;
        }
        if(projection!=null){
            projection.stop();
            projection=null;
        }
        //windowManager.removeView(text);
        windowManager.removeView(root);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        reader.setOnImageAvailableListener(null,null);
        File f=new File(getExternalFilesDir(null).getAbsolutePath()+"/translate.png");
        Image image=reader.acquireLatestImage();

        new Thread(() -> {
            FileOutputStream fos=null;
            try {
                fos = new FileOutputStream(f);
                final Image.Plane[] planes = image.getPlanes();
                Bitmap bitmap=Bitmap.createBitmap(planes[0].getRowStride()/planes[0].getPixelStride(), reader.getHeight(), Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(planes[0].getBuffer().rewind());
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                sendNotification(Utils.Bitmap.saveBitmap(bitmap,f));
                openStatusBar();
                //Translator.callTranslator(TranslateService.this, Utils.saveBitmap(bitmap,f));
            } catch (Exception e) {
                Logs.saveLog(e);
                e.printStackTrace();
            } finally {
                if(fos!=null){try{fos.close();}catch(IOException ioe){ioe.printStackTrace();}}
                if (image!=null){image.close();}
            }
            if(virtualDisplay!=null){
                virtualDisplay.release();
                virtualDisplay=null;
            }
        }).start();

        root.setVisibility(View.VISIBLE);
        //LayoutParams textParams=(LayoutParams)root.getLayoutParams();
        //textParams.x=((LayoutParams)root.getLayoutParams()).x;
        //textParams.y=((LayoutParams)root.getLayoutParams()).y+root.getHeight()/2;
        //windowManager.updateViewLayout(text, textParams);
        //text.setVisibility(View.VISIBLE);

        //handler.postDelayed(()->text.setVisibility(View.INVISIBLE),5000);
    }
}
