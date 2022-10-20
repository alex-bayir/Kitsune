package com.alex.ratingbar;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

public class RatingBar extends ImageView {
    public interface OnRatingBarChangeListener{
        void onRatingChanged(RatingBar ratingBar, double rating);
    }
    private LayerDrawable RTL,LTR;
    private double rating=0;
    private double max=5;
    private final ValueAnimator v=new ValueAnimator();
    private OnRatingBarChangeListener ratingChangeListener;

    public RatingBar(Context context){this(context,null);}
    public RatingBar(Context context, AttributeSet attrs){this(context, attrs,0);}
    public RatingBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        v.addUpdateListener(animation->getDrawable().setLevel((int)animation.getAnimatedValue()));
        v.setDuration(500).setInterpolator(new AccelerateDecelerateInterpolator());
        setScaleType(ScaleType.CENTER_INSIDE);
        TypedArray a=getContext().obtainStyledAttributes(attrs,R.styleable.RatingBar);
        LTR=(LayerDrawable) a.getDrawable(R.styleable.RatingBar_srcLTR);
        if(LTR==null){LTR=(LayerDrawable)getContext().getDrawable(R.drawable.ic_stars_ltr);}
        RTL=(LayerDrawable) a.getDrawable(R.styleable.RatingBar_srcRTL);
        if(RTL==null){RTL=(LayerDrawable)getContext().getDrawable(R.drawable.ic_stars_rtl);}
        init(getLayoutDirection()==View.LAYOUT_DIRECTION_RTL ? RTL:LTR);
        setMax(attrs!=null ? attrs.getAttributeFloatValue("android","max",5):5);
        setRating(attrs!=null ? attrs.getAttributeFloatValue("android","rating",0):0,false);
    }

    public double getMax(){return max;}
    public void setMax(float max){this.max=max; setRating(rating);}
    public void setRating(double rating){setRating(rating,false);}
    public void setRating(double rating, boolean animate){this.rating=Math.max(Math.min(rating,this.max),0); if(animate){v.setIntValues(0,Math.max((int)(10000*RatingBar.this.rating/max),1)); v.start();}else{getDrawable().setLevel(Math.max((int)(10000*RatingBar.this.rating/max),1));}}
    public double getRating(){return rating;}

    public ValueAnimator getAnimator(){return v;}
    public void init(LayerDrawable RTL,LayerDrawable LTR){this.RTL=RTL; this.LTR=LTR;}
    public void init(LayerDrawable shape){setImageDrawable(shape); setRating(rating);}
    public LayerDrawable getLTRDrawable(){return LTR;}
    public LayerDrawable getRTLDrawable(){return RTL;}
    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        init(layoutDirection==View.LAYOUT_DIRECTION_RTL ? RTL:LTR);
    }

    public void setRatingChangeListener(OnRatingBarChangeListener listener){ratingChangeListener=listener;}
    public OnRatingBarChangeListener getRatingChangeListener(){return ratingChangeListener;}
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(ratingChangeListener!=null){
            setRating(Math.min((event.getX()-getPaddingStart())/(getWidth()-getPaddingStart()-getPaddingEnd()),1)*getMax());
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_CANCEL || action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_HOVER_EXIT){
                ratingChangeListener.onRatingChanged(this,getRating());
            }
            return true;
        }
        return super.onTouchEvent(event);
    }
}
