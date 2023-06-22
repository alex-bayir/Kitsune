package org.alex.kitsune.commons;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.widget.Scroller;
import com.google.android.material.textview.MaterialTextView;

public class ZoomTextView extends MaterialTextView {
    private ScaleGestureDetector scale_detector;
    private GestureDetector detector;
    private float scale=1.f;
    private float defaultSize;
    private float min=1.f;
    private float max=3.f;
    private Scroller scroller;
    private VelocityTracker tracker;


    public ZoomTextView(Context context) {
        super(context);
        initialize();
    }

    public ZoomTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public ZoomTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        scroller=new Scroller(getContext());
        defaultSize = getTextSize();
        scale_detector = new ScaleGestureDetector(getContext(), new ScaleListener());
        detector=new GestureDetector(getContext(),new GestureDetector.SimpleOnGestureListener());
        //setNestedScrollingEnabled(true);
        setMovementMethod(new ScrollingMovementMethod());
    }

    public void setGestureListener(GestureDetector.OnGestureListener listener){
        detector=new GestureDetector(getContext(),listener);
    }

    public void setZoomMax(float max_scale) {
        this.max = max_scale;
    }
    public void setZoomMin(float min_scale){
        this.min = min_scale;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    private float last=0;
    private int last_scroll=0;
    private static final int fling=600;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()){
            case MotionEvent.ACTION_DOWN -> {
                last=e.getY();
                if(!scroller.isFinished()){scroller.abortAnimation();}
            }
            case MotionEvent.ACTION_MOVE -> {
                getParent().requestDisallowInterceptTouchEvent(canScrollVertically((int)((last-e.getY())*100))); last=e.getY();
            }
            case MotionEvent.ACTION_UP -> {
                getParent().requestDisallowInterceptTouchEvent(false);
                tracker.computeCurrentVelocity(fling);
                int v=(int)tracker.getYVelocity();
                if(Math.abs(v)>fling){
                    scroller.fling(0,last_scroll,0,(int)(-v*1.5),0,0,0,(getLineCount()+1)*getLineHeight()-getHeight());
                }

            }
        }
        scale_detector.onTouchEvent(e);
        detector.onTouchEvent(e);
        super.onTouchEvent(e);
        if(tracker==null){tracker=VelocityTracker.obtain();}
        tracker.addMovement(e);
        last_scroll=getScrollY();
        return true;
    }

    @Override
    public void computeScroll() {
        if(scroller.computeScrollOffset()){
            scrollTo(0,last_scroll=scroller.getCurrY());
            postInvalidate();
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scale = Math.max(min, Math.min(scale*detector.getScaleFactor(), max));
            setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultSize * scale);
            return true;
        }
    }
}
