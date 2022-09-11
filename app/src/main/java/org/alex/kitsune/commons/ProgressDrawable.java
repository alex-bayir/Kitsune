package org.alex.kitsune.commons;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ProgressDrawable extends Drawable{
    private int[] colors;
    private long max=0;
    private long progress=0;
    private boolean inverse=false;
    protected final Paint paint=new Paint();
    public ProgressDrawable(){this(new int[]{0xff0000ff,0xff00ffff,0xff00ff00,0xffffff00,0xffff0000,0xffff00ff});}
    public ProgressDrawable(int color){this(new int[]{color});}
    public ProgressDrawable(int[] colors){this.colors=colors;}
    public ProgressDrawable setColors(int color){return setColors(new int[]{color});}
    public ProgressDrawable setColors(int[] colors){
        this.colors=colors;
        return this;
    }
    public ProgressDrawable setMax(long max){
        this.max=max;
        return this;
    }
    public ProgressDrawable setProgress(long progress){
        this.progress=progress;
        return this;
    }
    public ProgressDrawable setInverse(boolean inverse){
        this.inverse=inverse;
        return this;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int height=getBounds().height();
        int width=getBounds().width();
        float h2=height/2f;
        long p=Math.min(max,progress);
        float len=max!=0 ? width/(float)max : 0;
        paint.setStrokeWidth(height);
        if(p>0 && len>0){
            if(colors.length==1){
                paint.setColor(colors[0]);
                if(inverse){
                    canvas.drawLine(width,h2,width-len*p,h2,paint);
                }else{
                    canvas.drawLine(0,h2,len*p,h2,paint);
                }
            }else{
                if(inverse){
                    for(int i=1;i<=p;i++){
                        paint.setColor(colors[i%colors.length]);
                        canvas.drawLine(width-len*(i-1),h2,width-len*(i),h2,paint);
                    }
                }else{
                    for(int i=1;i<=p;i++){
                        paint.setColor(colors[i%colors.length]);
                        canvas.drawLine(len*(i-1),h2,len*(i),h2,paint);
                    }
                }
            }
        }
    }

    @Override public void setAlpha(int alpha){paint.setAlpha(alpha); invalidateSelf();}
    @Override public void setColorFilter(@Nullable ColorFilter colorFilter){}
    @Override public int getOpacity(){return PixelFormat.TRANSPARENT;}
}
