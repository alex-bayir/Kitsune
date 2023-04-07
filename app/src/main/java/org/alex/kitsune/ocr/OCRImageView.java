package org.alex.kitsune.ocr;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.utils.Utils;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class OCRImageView extends PhotoView {
    static TextRecognizer recognizer;
    private File save;
    static HashMap<File,org.alex.kitsune.ocr.Text> texts=new HashMap<>();
    static String last_lang=null;
    private String getTarget_lang(){
        String lang=Translate.getTarget_lang(getContext());
        if(!Objects.equals(lang,last_lang)){
            texts.clear();
        }
        return last_lang=lang;
    }

    public static void init(){
        if(recognizer==null){
            recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        }
    }
    public OCRImageView(Context context) {
        super(context);
        init(context);
    }

    public OCRImageView(Context context, AttributeSet attr) {
        super(context, attr);
        init(context);
    }

    public OCRImageView(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);
        init(context);
    }

    private void init(Context context){
        initPaints();
    }

    public void setImageDrawable(Drawable drawable,File file){
        setImageDrawable(drawable);
        save=file;
    }
    TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    Paint ok=new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint error=new Paint(Paint.ANTI_ALIAS_FLAG);
    private void initPaints(){
        paint.setColor(0xFF000000);
        paint.setTextSize(8 * getResources().getDisplayMetrics().density);
        paint.setTextAlign(Paint.Align.CENTER);
        ok.setColor(0x9FFFFFFF);
        error.setColor(0x3FFF0000);
    }
    private final Rect tmp=new Rect();
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(show){
            org.alex.kitsune.ocr.Text text=texts.get(save);
            if(text!=null && getDrawable() instanceof BitmapDrawable bd){
                float scaleX=getWidth()/(float)bd.getBitmap().getWidth();
                float scaleY=getHeight()/(float)bd.getBitmap().getHeight();
                for(org.alex.kitsune.ocr.Text.Group group:text.getGroups()){
                    tmp.set(group.getBoundingBox());
                    scale(tmp,scaleX,scaleY);
                    if(group.isTranslated()){
                        canvas.drawRect(tmp,ok);
                        drawText(canvas,tmp,group.getTranslated(),paint);
                    }else{
                        canvas.drawRect(tmp,error);
                    }
                }
            }
        }
    }

    private boolean show=false; // static is important for save state on invalidate when activity paused
    public void show(){
        init();
        show=true;
        org.alex.kitsune.ocr.Text text=texts.get(save);
        if(text==null){
            if(getDrawable() instanceof BitmapDrawable bd){
                recognize(bd.getBitmap(), this::translate);
            }
        }
        if(text!=null){
            translate(text);
        }
    }

    public void hide(){
        show=false;
        invalidate();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        if(show){
            show();
        }
    }

    private int count=0;
    public void recognize(Bitmap bitmap,Callback<org.alex.kitsune.ocr.Text> callback){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity)getContext()).getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        int view_height = displayMetrics.heightPixels/2;
        //int view_height = height/Math.round(height/(float)(displayMetrics.heightPixels/2))+1;
        new Thread(()->{
            int width=bitmap.getWidth();
            int height=bitmap.getHeight();
            LinkedList<Text> blocks=new LinkedList<>();
            int i=0; count=0;
            while(i<height){
                count++;
                Rect rect=new Rect(0,i,width,Math.min(i+view_height,height));
                InputImage image=InputImage.fromBitmap(Bitmap.createBitmap(bitmap,rect.left,rect.top,rect.right-rect.left,rect.bottom-rect.top),0);
                recognizer.process(image).addOnSuccessListener(text -> {
                    blocks.add(text);
                    for(Text.TextBlock block:text.getTextBlocks()){
                        block.getBoundingBox().offset(rect.left,rect.top);
                    }
                    if(--count==0){
                        org.alex.kitsune.ocr.Text grouped=org.alex.kitsune.ocr.Text.create(collect(blocks).getTextBlocks());
                        texts.put(save,grouped);
                        new Handler(Looper.getMainLooper()).post(()->{
                            callback.call(grouped);
                        });
                    }
                }).addOnFailureListener(e -> e.printStackTrace());
                i+=view_height;
            }
        }).start();
    }

    private void translate(org.alex.kitsune.ocr.Text text){
        String target_lang=getTarget_lang();
        if(!text.isTranslated()){
            count=0;
            for(org.alex.kitsune.ocr.Text.Group group:text.getGroups()){
                if(!group.isTranslated()){
                    count++;
                    new Translate(target_lang).addOnCompleteListener(s->{
                        group.setTranslated(s.getTranslated());
                        if(--count==0){
                            new Handler(Looper.getMainLooper()).post(()->invalidate());
                        }
                    }).translate(group.getText());
                }
            }
        }
    }
    private static void drawText(Canvas canvas,Rect bounds,String text,TextPaint paint){
        StaticLayout l=StaticLayout.Builder.obtain(text, 0,text.length(),paint,bounds.width()).build();
        canvas.save();
        switch (paint.getTextAlign()){
            case CENTER -> canvas.translate(bounds.centerX(),bounds.top+(bounds.height()-l.getHeight())/2f);
            case LEFT-> canvas.translate(bounds.left,bounds.top+(bounds.height()-l.getHeight())/2f);
            case RIGHT-> canvas.translate(bounds.right,bounds.top+(bounds.height()-l.getHeight())/2f);
        }
        l.draw(canvas);
        canvas.restore();
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

    private static void scale(Rect rect,float scaleX,float scaleY){
        rect.left=(int)(rect.left*scaleX);
        rect.top=(int)(rect.top*scaleY);
        rect.right=(int)(rect.right*scaleX);
        rect.bottom=(int)(rect.bottom*scaleY);
    }

    public org.alex.kitsune.ocr.Text.Group getTextBlock(float x,float y){
        org.alex.kitsune.ocr.Text text=texts.get(save);
        if(text!=null && text.getGroups()!=null && getDrawable() instanceof BitmapDrawable bd){
            float scaleX=getWidth()/(float)bd.getBitmap().getWidth();
            float scaleY=getHeight()/(float)bd.getBitmap().getHeight();
            x=x/scaleX; y=y/scaleY;
            for(org.alex.kitsune.ocr.Text.Group group:text.getGroups()){
                if(group.getBoundingBox().contains((int)x,(int)y)){
                    return group;
                }
            }
        }
        return null;
    }
    public boolean showDialogTranslateIfTextExists(float x,float y,boolean integrated){
        org.alex.kitsune.ocr.Text.Group group=getTextBlock(x,y);
        if(group!=null){
            Intent intent=null;
            for(ActivityInfo info:Utils.Translator.getTextTranslators(getContext(),resolveInfo->resolveInfo.activityInfo)){
                intent=new Intent(Intent.ACTION_PROCESS_TEXT)
                        .setComponent(new ComponentName(info.packageName,info.name))
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_PROCESS_TEXT,group.getText())
                        .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY,true);
            }
            if(intent==null || integrated){
                new DialogTranslate(getContext()).init(group.getText(),group.getTranslated()).show();
            }else{
                getContext().startActivity(intent);
            }
            return true;
        }
        return false;
    }
}
