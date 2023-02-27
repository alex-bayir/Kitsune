package org.alex.kitsune.ocr;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.Toast;
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
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.ui.reader.ReaderPageHolder;
import org.alex.kitsune.utils.LoadTask;
import org.alex.kitsune.utils.Utils;

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

    HashMap<File,Text> texts=new HashMap<>();
    public static void init(Context context){
        libnative=new File(context.getFilesDir().getAbsoluteFile()+"/lib_native/libtranslate_jni.so");
        Logs.e(context.getFilesDir().getAbsoluteFile()+"/lib_native/libtranslate_jni.so");
        if(!libnative.exists()){
            new LoadTask<String,String,Boolean>(){

                @Override
                protected void onProgressUpdate(String s) {
                    Toast.makeText(context,s,Toast.LENGTH_SHORT).show();
                }

                @Override
                protected Boolean doInBackground(String url) {
                    return LoadTask.loadInBackground(url,null,libnative,null,getHandler(),false);
                }
            }.start("https://github.com/alex-bayir/Kitsune/tree/master/app/lib/libtranslate_jni.so");
        }else{
            init();
        }
    }

    public static void init(){
        try{
            System.load(libnative.getAbsolutePath());
            Logs.e("loaded");
        }catch (Throwable e){
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
                        if(show){
                            show();
                        }
                    }
                }
            }).addOnFailureListener(e -> e.printStackTrace());
            i+=view_height;
        }
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
            Drawable drawable=Drawable.createFromPath(save.getAbsolutePath());
            if(drawable instanceof BitmapDrawable bd){
                drawable=ReaderPageHolder.adapt_size(bd,save);
            }
            setImageDrawable(drawable);
            if(drawable instanceof Animatable animatable){
                animatable.start();
            }
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
    public static String arrToString(Object[] arr){
        String str="[";
        if(arr.length>0){
            str+=arr[0].toString();
        }
        for(int i=1;i<arr.length;i++){
            str+=","+arr[i].toString();
        }
        return str;
    }
}
