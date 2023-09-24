package com.alex.threestates;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textview.MaterialTextView;

public class ThreeStatesTextView extends MaterialTextView {
    private State state=State.Default;
    public ThreeStatesTextView(@NonNull Context context){this(context,null);}

    public ThreeStatesTextView(@NonNull Context context, AttributeSet attrs) {
        this(context, attrs,com.google.android.material.R.attr.checkedTextViewStyle);
    }

    public ThreeStatesTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    public void init(Context context){
        Drawable d=State.getStateDrawable(context);
        d.setBounds(0,0,d.getCurrent().getIntrinsicWidth(),d.getCurrent().getIntrinsicHeight());
        setCompoundDrawables(null,null,d,null);
    }

    public State getState(){return this.state;}
    public void setState(int state){setState(State.valueOf(state));}
    public void setState(State state){
        if(this.state!=state){
            this.state=state;
            refreshDrawableState();
        }
    }


    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] states=super.onCreateDrawableState(extraSpace+1);
        mergeDrawableStates(states,State.getState(state));
        return states;
    }
}

