package org.alex.kitsune.ocr;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import org.alex.kitsune.R;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.ui.reader.ReaderPageHolder;
import org.alex.kitsune.utils.LoadTask;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


public class OCRImageView extends PhotoView {

    static TranslatorOptions options;
    static Translator translator;
    static TextRecognizer recognizer;
    private Text text;
    private File save;
    private static File libnative=null;
    private static String channelID="download";
    private static int notificationId="libtranslate_jni.so".hashCode();

    HashMap<File,Text> texts=new HashMap<>();
    public static void init(Context context){
        libnative=new File(context.getFilesDir().getAbsoluteFile()+"/lib_native/libtranslate_jni.so");
        if(!libnative.exists()){
            NotificationManagerCompat notificationManagerCompat=NotificationManagerCompat.from(context);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                notificationManagerCompat.createNotificationChannel(new NotificationChannel(channelID,channelID, NotificationManager.IMPORTANCE_DEFAULT));
            }
            new LoadTask<String,String,Boolean>(){
                public static int parseProgress(String progress){
                    if(progress.endsWith("%")){
                        return (int)Float.parseFloat(progress.substring(0,progress.length()-1));
                    }else{
                        return Integer.parseInt(progress.substring(0,progress.length()-2));
                    }
                }
                @Override
                protected void onProgressUpdate(String s) {
                    NotificationCompat.Builder builder =
                            new NotificationCompat.Builder(context)
                                    .setSmallIcon(R.drawable.ic_launcher)
                                    .setContentTitle("Downloading libtranslate_jni.so")
                                    .setContentText("Downloading native library for text translating")
                                    .setChannelId(channelID)
                                    .setSilent(true)
                                    .setOngoing(true)
                                    .setProgress(100,parseProgress(s),false);
                    NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                    notificationManager.notify(notificationId, builder.build());
                }

                @Override
                protected void onFinished(Boolean aBoolean) {
                    notificationManagerCompat.cancel(notificationId);
                }

                @Override
                protected void onBraked(Throwable throwable) {
                    notificationManagerCompat.cancel(notificationId);
                }

                @Override
                protected Boolean doInBackground(String url) {
                    return LoadTask.loadInBackground(url,null,libnative,null,getHandler(),false);
                }
            }.start("https://raw.githubusercontent.com/alex-bayir/Kitsune/master/app/lib/libtranslate_jni.so");
        }else{
            init();
        }
    }

    public static void init(){
        try{
            System.load(libnative.getAbsolutePath());
            Logs.e("loaded");
        }catch (Throwable e){
            libnative.delete();
            e.printStackTrace();
        }
        recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        options=new TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(TranslateLanguage.RUSSIAN).build();
        translator=Translation.getClient(options);
        DownloadConditions conditions=new DownloadConditions.Builder().build();
        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    Logs.e("downloaded");
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                });
    }
    public OCRImageView(Context context) {
        super(context);
    }

    public OCRImageView(Context context, AttributeSet attr) {
        super(context, attr);
    }

    public OCRImageView(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);
    }


    public void setImageDrawable(Drawable drawable,File file){
        setImageDrawable(drawable);
        save=file;
    }

    private int count=0;
    private boolean show=false;
    public void recognize(Bitmap bitmap){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity)getContext()).getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        int view_height = displayMetrics.heightPixels;
        Logs.e(view_height);
        if(view_height==0 || bitmap.getHeight()==0 || bitmap.getWidth()==0){
            Logs.e("wrong condition");
            return;
        }
        new Thread(()->{
            int width=bitmap.getWidth();
            int height=bitmap.getHeight();
            text=null;
            LinkedList<Text> blocks=new LinkedList<>();
            int i=0; count=0;
            while(i<height){
                count++;
                Rect rect=new Rect(0,i,width,Math.min(i+view_height,height));
                InputImage image=InputImage.fromBitmap(Bitmap.createBitmap(bitmap,rect.left,rect.top,rect.right-rect.left,rect.bottom-rect.top),0);
                recognizer.process(image).addOnSuccessListener(text -> {
                    if(text==null){
                        Logs.e("Text is null");
                    }else{
                        blocks.add(text);
                        for(Text.TextBlock block:text.getTextBlocks()){
                            block.getBoundingBox().offset(rect.left,rect.top);
                        }
                        if(--count==0){
                            this.text=collect(blocks);
                            texts.put(save,this.text);
                            new Handler(Looper.getMainLooper()).post(()->{
                                if(show){
                                    show();
                                }
                            });
                        }
                    }
                }).addOnFailureListener(e -> e.printStackTrace());
                i+=view_height;
            }
        }).start();
    }

    public void show(){
        show=true;
        if(text==null){
            text=texts.get(save);
            if(text==null){
                if(getDrawable() instanceof BitmapDrawable bd){
                    recognize(bd.getBitmap());
                }
            }
        }
        if(text!=null){
            if(getDrawable() instanceof BitmapDrawable bd && bd.getBitmap()!=null){
                Canvas canvas=new Canvas(bd.getBitmap());
                Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setColor(0xFF000000);
                paint.setTextSize(22);
                Paint ok=new Paint(Paint.ANTI_ALIAS_FLAG);
                ok.setColor(0x7FFFFFFF);
                Paint error=new Paint(Paint.ANTI_ALIAS_FLAG);
                error.setColor(0x3FFF0000);
                for(Text.TextBlock block:text.getTextBlocks()){
                    translator.translate(block.getText()).addOnSuccessListener(s -> {
                        Rect rect=block.getBoundingBox();
                        int lines=countLines(block.getText());
                        canvas.drawRect(rect,ok);
                        canvas.drawText(s,rect.left,rect.top+rect.height()/lines,paint);
                    }).addOnFailureListener(e -> {
                        canvas.drawRect(block.getBoundingBox(),error);
                    });

                }
                bd.invalidateSelf();
            }
        }
    }

    private static int countLines(String str){
        String[] lines = str.split("\r\n|\r|\n");
        return  lines.length;
    }
    public void hide(){
        show=false;
        if(text!=null && save!=null && getDrawable() instanceof BitmapDrawable){
            ReaderPageHolder.loadDrawable(save,drawable->{
                setImageDrawable(drawable);
                if(drawable instanceof Animatable animatable){
                    animatable.start();
                }
            });
        }
    }


    private static Text collect(List<Text> texts){
        StringBuilder str=new StringBuilder();
        List<Text.TextBlock> list=new LinkedList<>();
        for(Text text:texts){
            str.append(text.getText()).append("\n");
            list.addAll(text.getTextBlocks());
        }
        return new Text(str.toString(),list);
    }
}
