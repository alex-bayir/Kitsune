package org.alex.kitsune.commons;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import org.jetbrains.annotations.NotNull;

public final class SwipeRemoveHelper extends ItemTouchHelper.Callback {
    private static final int MOVEMENT_FLAGS=makeMovementFlags(0, ItemTouchHelper.START | ItemTouchHelper.END);
    private final Drawable background;
    private final Drawable icon;
    private final OnItemRemovedListener listener;
    private final int padding;

    public interface OnItemRemovedListener {
        void onItemRemoved(int position);
    }
    public SwipeRemoveHelper(Context context, int background, int icon, int padding, OnItemRemovedListener listener){
        this(ContextCompat.getDrawable(context,background),ContextCompat.getDrawable(context,icon),(int)dpToPx(context.getResources(), padding),listener);
    }
    public SwipeRemoveHelper(Drawable background,Drawable icon,int padding,OnItemRemovedListener listener){
        this.background=background;
        this.icon=icon;
        this.padding=padding;
        this.listener=listener;
    }

    public static void setup(RecyclerView rv,SwipeRemoveHelper srh){
        (new ItemTouchHelper(srh)).attachToRecyclerView(rv);
    }

    @Override
    public int getMovementFlags(@NonNull @NotNull RecyclerView recyclerView, @NonNull @NotNull RecyclerView.ViewHolder viewHolder) {
        return MOVEMENT_FLAGS;
    }

    @Override
    public boolean onMove(@NonNull @NotNull RecyclerView recyclerView, @NonNull @NotNull RecyclerView.ViewHolder viewHolder, @NonNull @NotNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull @NotNull RecyclerView.ViewHolder viewHolder, int direction) {
        if(listener!=null){listener.onItemRemoved(viewHolder.getBindingAdapterPosition());}
    }

    @Override
    public void onChildDraw(@NonNull @NotNull Canvas c, @NonNull @NotNull RecyclerView recyclerView, @NonNull @NotNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if(viewHolder.getBindingAdapterPosition()!=-1){
            View v=viewHolder.itemView;
            if(dX!=0){
                background.setBounds(v.getLeft(),v.getTop(),v.getRight(),v.getBottom());
                background.draw(c);
                int width=v.getBottom()-v.getTop();
                int iconWidth=icon.getIntrinsicWidth();
                int top=v.getTop()+((width-iconWidth)/2);
                icon.setBounds(dX<0 ? v.getRight()-padding-iconWidth : padding, top, dX<0 ? v.getRight()-padding : padding+iconWidth, iconWidth+top);
                icon.draw(c);
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    public static float dpToPx(Resources res,float dp){return res.getDisplayMetrics().density*dp;}
}
