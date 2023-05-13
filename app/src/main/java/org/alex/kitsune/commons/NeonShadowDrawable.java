package org.alex.kitsune.commons;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

public class NeonShadowDrawable extends Drawable implements Animatable {
    public static final int[] rainbow=new int[]{0xff0000ff,0xff00ffff,0xff00ff00,0xffffff00,0xffff0000,0xffff00ff,0xff0000ff};
    private final int[] colors;
    private final float corners;
    private final int padding;
    private final Paint neon;
    private final Paint background;
    ValueAnimator animator;
    public static NeonShadowDrawable Rainbow(int background, float corners, int padding, float scale_elevation){
        return new NeonShadowDrawable(background,rainbow,corners,padding,padding*scale_elevation);
    }
    public static NeonShadowDrawable Rainbow(float corners, int padding, float scale_elevation){
        return new NeonShadowDrawable(0,rainbow,corners,padding,padding*scale_elevation);
    }
    public NeonShadowDrawable(int background, int[] colors, float corners, int padding, float elevation){
        this.colors=colors;
        this.padding=padding;
        this.corners=corners;
        this.neon=new Paint(Paint.ANTI_ALIAS_FLAG);
        this.neon.setStyle(Paint.Style.FILL);
        this.neon.setShadowLayer(elevation,0,0, Color.BLACK);
        this.background=new Paint(Paint.ANTI_ALIAS_FLAG);
        this.background.setStyle(Paint.Style.FILL);
        this.background.setColor(background);
        this.animator=ValueAnimator.ofFloat(0,colors.length);
        initAnimator(100,2000);
    }
    public void setToView(View view,boolean animate){
        view.setBackground(this);
        view.setPadding(padding,padding,padding,padding);
        if(animate){start();}
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
        super.onBoundsChange(bounds);
        neon.setShader(new SweepGradient(bounds.centerX(),bounds.centerY(), colors,null));
    }

    RectF rect=new RectF(getBounds());
    @Override
    public void draw(Canvas canvas) {
        rect.set(getBounds());
        rect.inset(padding,padding);
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
}
