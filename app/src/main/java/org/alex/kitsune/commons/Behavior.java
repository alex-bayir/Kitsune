package org.alex.kitsune.commons;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;


import static org.alex.kitsune.utils.Utils.inRange;

public class Behavior <V extends View> extends CoordinatorLayout.Behavior<V> {
    private int maxY,maxX;
    public Behavior(){super();}
    public Behavior(Context context, AttributeSet attributeSet){super(context,attributeSet);}

    public void onScroll(V child,int dx,int dy){
        child.setTranslationY(inRange(0,child.getTranslationY()+dy/2,maxY));
        child.setTranslationX(inRange(0,child.getTranslationX()+dy/2,maxX));
    }
    @Override
    public boolean onLayoutChild(@NonNull CoordinatorLayout parent, @NonNull V child, int layoutDirection) {
        maxX=child.getMeasuredWidth()+((ViewGroup.MarginLayoutParams) child.getLayoutParams()).rightMargin;
        maxY=child.getMeasuredHeight()+((ViewGroup.MarginLayoutParams) child.getLayoutParams()).bottomMargin;
        return super.onLayoutChild(parent, child, layoutDirection);
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View directTargetChild, @NonNull View target, int nestedScrollAxes, int type) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }
    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
        onScroll(child,dxConsumed,dyConsumed);
    }
}
