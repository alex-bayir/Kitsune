package org.alex.kitsune.commons;

import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.function.Function;

public class PhotoCollageDrawable extends Drawable{
    private final Drawable[] drawables;
    private int width=-1,height=-1,alpha=255;

    public PhotoCollageDrawable(Drawable insteadofnull,int max,Function<Void,Integer> count,Function<Integer,Drawable> get){
        if(insteadofnull==null){throw new NullPointerException("insteadofnull cannot be null");}
        this.drawables=new Drawable[Math.min(count.apply(null),max)];
        for(int i=0;i<this.drawables.length;i++){
            this.drawables[i]=get.apply(i); if(drawables[i]==null){drawables[i]=insteadofnull;}
        }
    }
    @Override
    public void draw(@NonNull Canvas canvas) {
        switch (drawables.length){
            case 0: break;
            case 1: setBounds(drawables[0],width=getBounds().width(),height=getBounds().height(),0,0,false,1).draw(canvas); break;
            case 2:
                setBounds(drawables[0],(width=getBounds().width())/2,height=getBounds().height(),0,0).draw(canvas);
                setBounds(drawables[1],(width=getBounds().width())/2,height=getBounds().height(),getBounds().width()/2,0).draw(canvas);
                break;
            default: create(canvas,drawables,alpha,createPath((width=getBounds().width())/(float)(drawables.length-1),height=getBounds().height())); break;
        }
    }
    @Override public void setAlpha(int alpha){this.alpha=alpha; invalidateSelf();}
    @Override public int getAlpha(){return alpha;}

    @Override public void setColorFilter(@Nullable ColorFilter colorFilter){}
    @Override public int getOpacity(){return PixelFormat.TRANSPARENT;}
    @Override public int getIntrinsicWidth(){return width;}
    @Override public int getIntrinsicHeight(){return height;}

    public Bitmap getBitmap(){Bitmap bitmap=Bitmap.createBitmap(getIntrinsicWidth(),getIntrinsicHeight(),Bitmap.Config.ARGB_8888); draw(new Canvas(bitmap)); return bitmap;}
    public Drawable getDrawable(int i){return drawables[i];}

    private static Path createPath(float width,float height){
        Path p=new Path();
        p.moveTo(0,0);
        p.rLineTo(width,0);
        p.rLineTo(-width,height);
        p.rLineTo(-width,0);
        p.rLineTo(width,-height);
        p.close();
        return p;
    }
    private static void create(Canvas canvas,Drawable[] drawables,int alpha,Path p){
        RectF rect=new RectF(canvas.getClipBounds());
        float w=rect.width()/(float)(drawables.length-1);
        p.computeBounds(rect,false);
        for(int i=0;i<drawables.length;i++){
            canvas.save();
            canvas.translate(i*w,0);
            canvas.clipPath(p);
            drawables[i].setAlpha(alpha);
            setBounds(drawables[i], (int)(rect.width()),(int)(rect.height()),(int)(-rect.width()/2),0).draw(canvas);
            canvas.restore();
        }
    }

    private static Drawable setBounds(Drawable d,int min_width,int min_height,int left,int top){return setBounds(d,min_width,min_height,left,top,d instanceof BitmapDrawable,d instanceof BitmapDrawable ? 1:0.5);}
    private static Drawable setBounds(Drawable d,int min_width,int min_height,int left,int top, boolean max,double additionalScale){
        double scale=(max ? Math.max(min_height/(double)d.getMinimumHeight(),min_width/(double)d.getMinimumWidth()) : Math.min(min_height/(double)d.getMinimumHeight(),min_width/(double)d.getMinimumWidth()))*additionalScale;
        left+=(min_width-d.getMinimumWidth()*scale)/2; top+=(min_height-d.getMinimumHeight()*scale)/2;
        d.setBounds(left,top,(int)(d.getMinimumWidth()*scale)+left,(int)(d.getMinimumHeight()*scale)+top);
        return d;
    }
    private static int getPos(int i,int len,boolean p){
        return p ? (i<len/2 ? len-(i+1)*2 : (i-len/2)*2) : i;
    }
}
