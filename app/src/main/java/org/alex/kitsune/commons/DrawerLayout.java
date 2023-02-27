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
    ViewDragHelper helper;
    private int drawerWidth;
    private int drawerHeight;
    private int contentWidth;
    private int contentHeight;
    private float startX;
    private float startY;
    private final int deltaVelocity=5;
    private float x=0;
    private float y=0;

    private boolean opened;
    private int duration=300;

    ArrayList<DrawerListener> listeners=new ArrayList<>();
    private boolean bothSide=false;
    private int skipFirst=3;
    private int skip=skipFirst;

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
        return helper.isRTL()?-1:1;
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
        helper=new ViewDragHelper(this);
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
        helper.setRtl(getLayoutDirection()==View.LAYOUT_DIRECTION_RTL);
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
                drawer.animate().x(helper.slide(opened,event.getX()-startX))
                        .setUpdateListener(animation -> onSlide(drawer,helper.offset())).setDuration(0).start();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(helper.shouldOpen(opened)){
                    openDrawer();
                }else{
                    closeDrawer();
                }
                break;
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean intercept = false;
        float velocityX = event.getX() - x;
        float velocityY = event.getY() - y;
        x = event.getX();
        y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = x;
                startY = y;
                skip=skipFirst;
                if(opened && !helper.is_dx_in_range(x)){
                    intercept=true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(skip--<=0){
                    final float dx = x - startX;
                    final float dy = y - startY;
                    if(abs(dx)>abs(dy) && abs(velocityX)>abs(velocityY)+deltaVelocity){
                        if(bothSide && !opened){helper.setRtl(dx<0);} // both sides
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
    public void openDrawer() {
        openDrawer(duration);
    }
    public void openDrawer(int duration) {
        opened = true;
        drawer.animate().x(helper.get_open())
                .setUpdateListener(animation -> onSlide(drawer,helper.offset()))
                .withEndAction(()->onOpened(drawer)).setDuration(duration).start();
    }

    public void closeDrawer() {
        closeDrawer(duration);
    }
    public void closeDrawer(int duration) {
        opened = false;
        drawer.animate().x(helper.get_close())
                .setUpdateListener(animation -> onSlide(drawer,helper.offset()))
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

    public static class ViewDragHelper{
        boolean rtl=false;
        private int min;
        private int max;
        int d_width;
        int p_width;
        View drawer;
        public ViewDragHelper(DrawerLayout drawer){
            this.drawer=drawer.getDrawer();
            drawer.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                p_width=right-left;
                d_width=this.drawer.getWidth();
                setRtl(isRTL());
            });
        }
        public boolean isRTL(){return rtl;}
        public void setRtl(boolean rtl){
            this.rtl=rtl;
            max=rtl?p_width:0;
            min=max-d_width;
        }
        public float offset(){
            return offset(drawer.getX());
        }
        public float offset(float x){
            return (isRTL()?max-range(x):range(x)-min)/(float)d_width;
        }
        public float slide(boolean opened,float dx){
            return range(dx+(opened?get_open():get_close()));
        }
        public float range(float x){
            return max(min(x,max),min);
        }
        public boolean is_in_range(float x){
            return min<=x && x<=max;
        }
        public boolean is_dx_in_range(float dx){
            return is_in_range(dx-(isRTL()?0:d_width));
        }
        public float get_open(){
            return isRTL()?min:max;
        }
        public float get_close(){
            return isRTL()?max:min;
        }
        public boolean shouldOpen(boolean opened){
            return shouldOpen(opened,offset());
        }
        public static boolean shouldOpen(boolean opened,float offset){
            return offset>(opened?0.75f:0.25f);
        }
    }
}