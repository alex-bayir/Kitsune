package org.alex.kitsune.commons;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener;
import java.util.ArrayList;
import org.alex.kitsune.R;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.abs;

public class DrawerLayout extends FrameLayout {

    private View drawer;
    private View content;

    private int drawerWidth;
    private int drawerHeight;
    private int contentWidth;
    private int contentHeight;


    private float startX;
    private float startY;

    private float velocityX;
    private float velocityY;

    private static int defaultMinimalVelocity=30;
    private int minimalVelocity=defaultMinimalVelocity;
    private float x=0;
    private float y=0;

    private boolean opened;

    public static final int LEFT=1;
    public static final int RIGHT=-1;

    private int direction=LEFT;

    private int duration=300;

    ArrayList<DrawerListener> listeners=new ArrayList<>();
    private boolean bothSide=false;

    public DrawerLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void addShadowingContent(){
        addShadowingContent(0.25f);
    }
    public void addShadowingContent(float minAlpha){
        addDrawerListener(new androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener() {
            @Override public void onDrawerSlide(View drawerView, float slideOffset) {
                content.setAlpha(max(1-slideOffset,minAlpha));
            }
        });
    }

    public void addHamburger(Activity activity, Toolbar toolbar){
        ActionBarDrawerToggle drawerToggle=new ActionBarDrawerToggle(activity, this, toolbar, R.string.navigation_drawer_open,  R.string.navigation_drawer_close);
        drawerToggle.setDrawerIndicatorEnabled(true);
        addDrawerListener(drawerToggle);
        drawerToggle.syncState();
    }

    public boolean isDrawerOpen(){
        return opened;
    }
    public void setBothSide(boolean bothSide) {
        this.bothSide = bothSide;
    }
    public boolean isBothSide(){
        return bothSide;
    }

    public int getDirection(){
        return direction;
    }

    public void setDuration(int duration){
        this.duration=duration;
    }
    public int getDuration(){
        return duration;
    }

    public View getContent(){
        return content;
    }

    public View getDrawer(){
        return drawer;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        content = getChildAt(0);
        drawer = getChildAt(1);
        drawer.setElevation(content.getElevation()+1);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        contentWidth = content.getMeasuredWidth();
        contentHeight = content.getMeasuredHeight();
        drawerWidth = drawer.getMeasuredWidth();
        drawerHeight = drawer.getMeasuredHeight();
    }

    @Override
    public void setLayoutDirection(int layoutDirection) {
        super.setLayoutDirection(layoutDirection);
        direction=getLayoutDirection()==View.LAYOUT_DIRECTION_RTL?RIGHT:LEFT;
    }
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        content.layout(0, 0, contentWidth, contentHeight);
        drawer.layout(-drawerWidth, 0, 0, drawerHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:

                float dx=event.getX()-startX;
                if(direction==LEFT){
                    x=max(min(dx-(opened ?0:drawerWidth),0),-drawerWidth);
                }else{
                    x=max(min(dx-(opened ?drawerWidth:0),0),-drawerWidth)+getWidth();
                }
                drawer.animate().x(x).setDuration(0).setListener(null).start();
                onSlide(drawer,offset(x));
                break;
            case MotionEvent.ACTION_UP:
                float w=(direction==LEFT?-x:drawerWidth-getWidth()+x);
                if (w > drawerWidth / 2f) {
                    closeDrawer();
                } else {
                    openDrawer();
                }
                break;
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean intercept = false;
        velocityX=event.getX()-x;
        velocityY=event.getY()-y;
        x = event.getX();
        y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = x;
                startY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                final float dx = x - startX;
                final float dy = y - startY;
                if(abs(dx)>abs(dy) && abs(velocityX)>abs(velocityY)+minimalVelocity){
                    if(bothSide && !opened){direction=dx<0?RIGHT:LEFT;} // both sides
                    if(isNeedIntercept(velocityX,velocityY)){
                        startX = x;
                        startY = y;
                        intercept=true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return intercept;
    }

    /** Get offset for drawer listeners
     * @param x current x coordinate of view (view.getX())
     * @return The new offset of this drawer within its range, from 0-1
     */
    private float offset(float x){
        return (direction==LEFT?drawerWidth+x:getWidth()-x)/drawerWidth;
    }

    private boolean isNeedIntercept(float velocityX,float velocityY){
        return bothSide || ((velocityX*direction>0 && !opened) || (velocityX*direction<0 && opened));
    }
    public void setMinimalVelocity(int velocity){
        minimalVelocity=velocity;
    }
    public void resetMinimalVelocity(){
        minimalVelocity=defaultMinimalVelocity;
    }
    public void openDrawer() {
        openDrawer(duration);
    }
    public void openDrawer(int duration) {
        opened = true;
        drawer.animate().x(direction==LEFT?0:getWidth()-drawerWidth)
                .setUpdateListener(animation -> onSlide(drawer,offset(drawer.getX())))
                .withEndAction(()->onOpened(drawer)).setDuration(duration).start();
    }

    public void closeDrawer() {
        closeDrawer(duration);
    }
    public void closeDrawer(int duration) {
        opened = false;
        drawer.animate().x(direction==LEFT?-drawerWidth:getWidth())
                .setUpdateListener(animation -> onSlide(drawer,offset(drawer.getX())))
                .withEndAction(()->onClosed(drawer)).setDuration(duration).start();
    }

    public void addDrawerListener(DrawerListener listener){
        if(listener!=null){
            listeners.add(listener);
        }
    }
    public void removeListener(DrawerListener listener){
        if(listener!=null){
            listeners.remove(listener);
        }
    }

    private void onSlide(View drawer,float offset){
        for(DrawerListener listener:listeners){
            listener.onDrawerSlide(drawer,offset);
        }
    }
    private void onOpened(View drawer){
        for(DrawerListener listener:listeners){
            listener.onDrawerOpened(drawer);
        }
    }
    private void onClosed(View drawer){
        for(DrawerListener listener:listeners){
            listener.onDrawerClosed(drawer);
        }
    }
}