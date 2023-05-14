package org.alex.kitsune.commons;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.ColorUtils;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class NeonShadowDrawable extends Drawable implements Animatable {
    public static final int[] rainbow=new int[]{0xff0000ff,0xff00ffff,0xff00ff00,0xffffff00,0xffff0000,0xffff00ff,0xff0000ff};
    private final int[] colors;
    private final float corners;
    private final int padding;
    private final Paint neon=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint background=new Paint(Paint.ANTI_ALIAS_FLAG);
    ValueAnimator animator;
    public static NeonShadowDrawable Rainbow(int background, ViewGroup corners_padding){
        return new NeonShadowDrawable(background,rainbow,getCornersChildCard(corners_padding,0),getPadding(corners_padding));
    }
    public static NeonShadowDrawable Rainbow(int background, float corners,View padding){
        return new NeonShadowDrawable(background,rainbow,corners,getPadding(padding));
    }
    public static NeonShadowDrawable Rainbow(int background, float corners, int padding){
        return new NeonShadowDrawable(background,rainbow,corners,padding);
    }
    public static NeonShadowDrawable Rainbow(float corners, int padding){
        return new NeonShadowDrawable(0,rainbow,corners,padding);
    }
    public NeonShadowDrawable(int background, int[] colors, float corners, int padding){
        this.colors=colors;
        this.corners=corners;
        this.neon.setStyle(Paint.Style.FILL);
        this.background.setStyle(Paint.Style.FILL);
        this.background.setColor(background);
        this.animator=ValueAnimator.ofFloat(0,colors.length);
        this.padding=padding;
        neon.setMaskFilter(new BlurMaskFilter(padding*0.75f, BlurMaskFilter.Blur.OUTER));
        initAnimator(100,2000);
    }
    public NeonShadowDrawable setToView(View view,boolean animate){
        view.setPadding(max(view.getPaddingStart(),padding),max(view.getPaddingTop(),padding),max(view.getPaddingEnd(),padding),max(view.getPaddingBottom(),padding));
        view.setBackground(this);
        if(animate){start();}
        return this;
    }
    public void setBackground(int color){
        this.background.setColor(color);
        invalidateSelf();
    }
    public int getPadding(){
        return padding;
    }

    public float getCorners() {
        return corners;
    }

    public int[] getColors() {
        return colors;
    }

    public void initAnimator(long delay, long duration){
        animator.removeAllUpdateListeners();
        int[] colors=new int[this.colors.length];
        final long[] delta = {System.currentTimeMillis()};
        animator.addUpdateListener(animation -> {
            if (System.currentTimeMillis() - delta[0] > delay) {
                float fraction = (Float)animation.getAnimatedValue();
                delta[0] = System.currentTimeMillis();
                for(int i=0;i<colors.length;i++){
                    colors[i]=ColorUtils.blendARGB(this.colors[(i+(int)fraction)%(this.colors.length-1)],this.colors[(i+1+(int)fraction)%(this.colors.length-1)],fraction-(int)fraction);
                }
                neon.setShader(new SweepGradient(getBounds().centerX(),getBounds().centerY(), colors,null));
                invalidateSelf();
            }
        });
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(Animation.INFINITE);
        animator.setDuration(duration);
    }
    @Override
    protected void onBoundsChange(Rect bounds) {
        rect.set(bounds);
        rect.inset(padding,padding);
        neon.setShader(new SweepGradient(bounds.centerX(),bounds.centerY(), colors,null));
    }

    RectF rect=new RectF(getBounds());
    @Override
    public void draw(Canvas canvas) {
        canvas.drawRoundRect(rect,corners,corners,neon);
        canvas.drawRoundRect(rect,corners,corners,background);
    }

    @Override
    public void setAlpha(int alpha) {
        neon.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        neon.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return neon.getAlpha()==0?PixelFormat.TRANSPARENT:neon.getAlpha()==255?PixelFormat.OPAQUE:PixelFormat.TRANSLUCENT;
    }

    @Override
    public void start() {
        animator.start();
    }

    @Override
    public void stop() {
        animator.end();
    }

    @Override
    public boolean isRunning() {
        return animator.isRunning();
    }

    public static int getPadding(View view){
        return min(view.getPaddingLeft(),min(view.getPaddingTop(),min(view.getPaddingRight(),view.getPaddingBottom())));
    }
    public static float getCornersChildCard(ViewGroup parent,float def){
        return parent.getChildCount()==1 && parent.getChildAt(0) instanceof CardView cv? cv.getRadius() : def;
    }
}
