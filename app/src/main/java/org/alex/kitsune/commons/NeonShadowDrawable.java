package org.alex.kitsune.commons;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class NeonShadowDrawable extends Drawable implements Animatable {
    public static final int[] rainbow=new int[]{0xff0000ff,0xff00ffff,0xff00ff00,0xffffff00,0xffff0000,0xffff00ff,0xff0000ff};
    private final int[] colors;
    protected final Rect paddings;
    private final Paint neon=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint background=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Function<RectF,Path> callback;
    protected Path path=new Path();
    private final RectF rect=new RectF(getBounds());
    protected final ValueAnimator animator;
    protected boolean ignoreAlphaShadow;
    protected boolean ignoreAlphaBackground;
    public NeonShadowDrawable(int background, int[] colors, Rect paddings, Function<RectF,Path> callback, boolean ignoreAlphaShadow, boolean ignoreAlphaBackground){
        this.colors=colors;
        this.paddings=paddings;
        this.neon.setStyle(Paint.Style.FILL);
        this.background.setStyle(Paint.Style.FILL);
        this.background.setColor(background);
        this.animator=ValueAnimator.ofFloat(0,colors.length);
        this.callback=callback;
        this.ignoreAlphaShadow=ignoreAlphaShadow;
        this.ignoreAlphaBackground=ignoreAlphaBackground;
        int max=max(paddings.left,max(paddings.top,max(paddings.right, paddings.bottom)));
        int min=min(paddings.left,min(paddings.top,min(paddings.right, paddings.bottom)));
        neon.setMaskFilter(new BlurMaskFilter((min>0? min:max)*0.75f, BlurMaskFilter.Blur.OUTER));
        initAnimator(100,2000);
    }
    private NeonShadowDrawable animate(boolean animate){
        if(animate){start();} return this;
    }
    public void setBackground(int color){
        this.background.setColor(color);
        invalidateSelf();
    }
    public android.graphics.Rect getPaddings(){
        return paddings;
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
        rect.left+=paddings.left;
        rect.top+=paddings.top;
        rect.right-=paddings.right;
        rect.bottom-=paddings.bottom;
        path=callback.apply(rect);
        neon.setShader(new SweepGradient(bounds.centerX(),bounds.centerY(), colors,null));
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawPath(path,neon);
        canvas.drawPath(path,background);
    }

    @Override
    public void setAlpha(int alpha) {
        if(!ignoreAlphaShadow){neon.setAlpha(alpha);}
        if(!ignoreAlphaBackground){background.setAlpha(alpha);}
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        neon.setColorFilter(colorFilter);
        background.setColorFilter(colorFilter);
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

    private static Rect getRectOf(ViewGroup parent,View child,Rect paddings){
        Rect rect=new Rect();
        child.getDrawingRect(rect);
        parent.offsetDescendantRectToMyCoords(child,rect);
        rect.left-=paddings.left;
        rect.top-=paddings.top;
        rect.right+=paddings.right;
        rect.bottom+=paddings.bottom;
        return rect;
    }
    public static void setTo(ViewGroup parent,int max_views,Function<Integer,NeonShadowDrawable> creator){
        NeonShadowDrawable[] array=new NeonShadowDrawable[max_views];
        for(int i=0;i<array.length;i++){array[i]=creator.apply(i);}
        setTo(parent,array);
    }
    public static void setTo(ViewGroup parent,NeonShadowDrawable[] array){
        LayerDrawable layers=new LayerDrawable(array);
        if(parent instanceof RecyclerView rv){
            rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull @NotNull RecyclerView recyclerView, int dx, int dy) {
                    for(int i=0;i<array.length;i++){
                        if(array[i]!=null){
                            if(i<parent.getChildCount()){
                                array[i].setBounds(NeonShadowDrawable.getRectOf(parent,parent.getChildAt(i),array[i].getPaddings()));
                            }else{
                                array[i].setBounds(-1000,-1000,-1000,-1000);
                            }
                            array[i].invalidateSelf();
                        }
                    }
                }
            });
        }else{
            parent.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                for(int i=0;i<array.length;i++){
                    if(array[i]!=null && i<parent.getChildCount()){
                        Rect bounds=NeonShadowDrawable.getRectOf(parent,parent.getChildAt(i),array[i].getPaddings());
                        layers.setLayerInset(i,bounds.left,bounds.top,right-left-bounds.right,bottom-top-bounds.bottom);
                        array[i].invalidateSelf();
                    }
                }
            });
        }
        parent.setBackground(layers);
    }

    public static class Builder{
        Function<RectF,Path> callback;
        int background=0;
        int[] colors=rainbow;
        Rect paddings=new Rect(Utils.toDP(4),Utils.toDP(4),Utils.toDP(4),Utils.toDP(4));
        private boolean ignoreAlphaShadow=false;
        private boolean ignoreAlphaBackground=false;
        public Builder(Function<RectF,Path> callback){
            this.callback=callback;
        }

        public Builder colors(int[] colors){
            if(colors==null){throw new IllegalArgumentException("Colors cannot be null");}
            this.colors=colors; return this;
        }
        public Builder background(int color){
            this.background=color; return this;
        }
        public Builder paddings(int left,int top,int right,int bottom){
            if(!(left>0 || top>0 || right>0 || bottom>0)){throw new IllegalArgumentException("Any Padding must be bigger than zero");}
            this.paddings=new Rect(left, top, right, bottom); return this;
        }
        public Builder padding(int padding){
            return paddings(padding,padding,padding,padding);
        }
        public Builder padding(float padding){
            return padding(Utils.toDP(padding));
        }
        public Builder paddingsFrom(View view){
            return paddings(view.getPaddingLeft(),view.getPaddingTop(),view.getPaddingRight(),view.getPaddingBottom());
        }
        public Builder setIgnoreAlphaShadow(boolean ignore){
            this.ignoreAlphaShadow=ignore; return this;
        }
        public Builder setIgnoreAlphaBackground(boolean ignore){
            this.ignoreAlphaBackground=ignore; return this;
        }
        public NeonShadowDrawable build(){
            return new NeonShadowDrawable(background,colors, paddings,callback,ignoreAlphaShadow,ignoreAlphaBackground);
        }
        public NeonShadowDrawable build(boolean animate){
            return build().animate(animate);
        }
        public void buildFrom(View view,boolean animate){
            paddingsFrom(view);
            buildTo(view,animate);
        }
        public void buildTo(View view, boolean animate){
            NeonShadowDrawable drawable=build();
            android.graphics.Rect paddings=drawable.getPaddings();
            view.setPadding(paddings.left,paddings.top,paddings.right,paddings.bottom);
            view.setBackground(drawable);
            if(animate){drawable.start();}
        }
    }
    public static class RoundRect extends Builder{
        public RoundRect(float corners){
            super(bounds -> {
                Path path=new Path();
                path.addRoundRect(bounds,corners,corners,Path.Direction.CW);
                path.close();
                return path;
            });
        }
    }
    public static class Circle extends Builder{
        public Circle(){
            super(bounds -> {
                Path path=new Path();
                path.addCircle(bounds.centerX(),bounds.centerY(),min(bounds.width(),bounds.height())/2f,Path.Direction.CW);
                path.close();
                return path;
            });
        }
    }
}
