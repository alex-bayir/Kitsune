package org.alex.kitsune.commons;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.AppCompatImageView;

public class AspectRatioImageView extends AppCompatImageView {
    public static int Width=0;
    public static int Height=1;
    protected int ratioMode=Width;
    protected double AspectRatio=1.4;

    public AspectRatioImageView(Context context){super(context);}

    public AspectRatioImageView(Context context, AttributeSet attributeSet){super(context, attributeSet);}

    public AspectRatioImageView(Context context, AttributeSet attributeSet, int i){super(context, attributeSet, i);}

    public AspectRatioImageView(Context context, ScaleType scaleType, Drawable drawable, double aspectRatio){
        this(context);
        AspectRatio=aspectRatio;
        setScaleType(scaleType);
        setImageDrawable(drawable);
    }
    public AspectRatioImageView(Context context, ScaleType scaleType, Drawable drawable){
        this(context,scaleType,drawable,drawable!=null ? drawable.getIntrinsicHeight()/(double)drawable.getIntrinsicWidth() : 1.4);
    }
    public void setAspectRatio(double ratio){
        if(AspectRatio!=ratio){
            AspectRatio=ratio;
            requestLayout();
        }
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width=View.MeasureSpec.getSize(widthMeasureSpec);
        int height=View.MeasureSpec.getSize(heightMeasureSpec);
        //super.onMeasure(View.MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec((int) (size * AspectRatio), MeasureSpec.EXACTLY));
        if(ratioMode==Height){
            //height=Math.max(height,getMinimumHeight());
            setMeasuredDimension((int) (height / AspectRatio),height);
        }else{
            //width=Math.max(width,getMinimumWidth());
            setMeasuredDimension(width,(int) (width * AspectRatio));
        }
    }
    public void setMode(int mode){
        checkMode(mode);
        if(ratioMode!=mode){
            ratioMode=mode;
            requestLayout();
        }
    }
    public int getMode(){return ratioMode;}
    private void checkMode(int mode){if(mode!=Width && mode!=Height){throw new IllegalStateException("no such mode like "+mode);}}

    public void setRatioAndMode(double ratio,int mode){
        checkMode(mode);
        if(ratioMode!=mode || AspectRatio!=ratio){
            ratioMode=mode;
            AspectRatio=ratio;
            requestLayout();
        }
    }
}
