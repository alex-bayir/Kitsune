package org.alex.kitsune.commons;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.view.View;
import android.view.animation.Animation;
import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;
import org.alex.kitsune.utils.Utils;

public class NeonShadowDrawable extends LayerDrawable {
    public static final int[] rainbow=new int[]{0xff0000ff,0xff00ffff,0xff00ff00,0xffffff00,0xffff0000,0xffff00ff,0xff0000ff};
    private final ShapeDrawable shadow;
    private final GradientDrawable background;
    private final int[] colors;
    public NeonShadowDrawable(@ColorInt int background,@ColorInt int[] colors, float corner, float elevation){
        this(colors,create(colors,corner,elevation,0,0),create(background,corner));
    }
    public NeonShadowDrawable(@ColorInt int background,@ColorInt int[] colors, float corner, float elevation, float centerX, float centerY){
        this(colors,create(colors,corner,elevation,centerX,centerY),create(background,corner));
    }
    public NeonShadowDrawable(ColorStateList background,@ColorInt int[] colors, float corner, float elevation){
        this(colors,create(colors,corner,elevation,0,0),create(background,corner));
    }
    public NeonShadowDrawable(ColorStateList background,@ColorInt int[] colors, float corner, float elevation, float centerX, float centerY){
        this(colors,create(colors,corner,elevation,centerX,centerY),create(background,corner));
    }
    public NeonShadowDrawable(int[] colors,ShapeDrawable shadow,GradientDrawable background){
        super(new Drawable[]{shadow,background});
        this.colors=colors;
        this.shadow=shadow;
        this.background=background;
    }
    public int[] getColors(){
        return colors;
    }
    public ShapeDrawable getShadow(){
        return shadow;
    }
    public GradientDrawable getBackground(){
        return background;
    }
    public void updateShadow(@ColorInt int[] colors,float centerX,float centerY){
        shadow.getPaint().setShader(new SweepGradient(centerX,centerY,colors,null));
    }
    private static ShapeDrawable create(@ColorInt int[] colors, float corner, float elevation, float centerX, float centerY){
        ShapeDrawable shadow=new ShapeDrawable();
        shadow.getPaint().setShadowLayer(elevation,0,0, Color.BLACK);
        shadow.getPaint().setShader(new SweepGradient(centerX,centerY,colors,null));
        shadow.setShape(new RoundRectShape(new float[]{corner,corner,corner,corner,corner,corner,corner,corner},null,null));
        return shadow;
    }

    private static GradientDrawable create(@ColorInt int color, float corner){
        GradientDrawable background=new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(corner);
        return background;
    }
    private static GradientDrawable create(ColorStateList color, float corner){
        GradientDrawable background=new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(corner);
        return background;
    }
    public static void setToView(View view, long duration){
        setToView(view,(int)Utils.toDP(4),duration);
    }
    public static void setToView(int background, View view, long duration){
        setToView(background,view,(int)Utils.toDP(4),duration);
    }
    public static void setToView(ColorStateList background, View view, long duration){
        setToView(background,view,(int)Utils.toDP(4),duration);
    }
    public static void setToView(View view, int padding, long duration){
        setToView(0xff1f1f1f,view,padding,duration);
    }
    public static void setToView(int background, View view, int padding, long duration){
        setToView(new NeonShadowDrawable(background,rainbow,(int)Utils.toDP(4),padding/2f),view,padding,duration);
    }
    public static void setToView(int background, int corner, View view, int padding, long duration){
        setToView(new NeonShadowDrawable(background,rainbow,corner,padding/2f),view,padding,duration);
    }
    public static void setToView(ColorStateList background, View view, int padding, long duration){
        setToView(new NeonShadowDrawable(background,rainbow,(int)Utils.toDP(4),padding/2f),view,padding,duration);
    }
    public static void setToView(NeonShadowDrawable drawable, View view, int padding,long duration){
        view.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            float centerX=(right-left)/2f-padding;
            float centerY=(bottom-top)/2f-padding;
            drawable.updateShadow(drawable.colors,centerX,centerY);
            drawable.setLayerInset(0,padding,padding,padding,padding);
            drawable.setLayerInset(1,padding,padding,padding,padding);
            v.setPadding(padding,padding,padding,padding);
            v.setBackground(drawable);
            if(duration>0){
                long invalidateDelay=100;
                final long[] deltaTime = {System.currentTimeMillis()};
                int[] c=drawable.getColors();
                int[] colors=new int[c.length];
                ValueAnimator animator=ValueAnimator.ofFloat(0,colors.length);
                animator.addUpdateListener(animation -> {
                    if (System.currentTimeMillis() - deltaTime[0] > invalidateDelay) {
                        float fraction = (Float)animation.getAnimatedValue();
                        deltaTime[0] = System.currentTimeMillis();
                        for(int i=0;i<colors.length;i++){
                            colors[i]=ColorUtils.blendARGB(c[(i+(int)fraction)%(c.length-1)],c[(i+1+(int)fraction)%(c.length-1)],fraction-(int)fraction);
                        }
                        drawable.updateShadow(colors,centerX,centerY);
                        drawable.shadow.invalidateSelf();
                    }
                });
                animator.setRepeatMode(ValueAnimator.REVERSE);
                animator.setRepeatCount(Animation.INFINITE);
                animator.setDuration(duration);
                animator.start();
            }
        });
    }
}
