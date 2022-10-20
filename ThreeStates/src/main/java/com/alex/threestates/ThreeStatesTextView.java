package com.alex.threestates;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textview.MaterialTextView;
import org.alex.threestates.R;

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
        StateListDrawable stateList=(StateListDrawable)context.getDrawable(R.drawable.states);
        Drawable dr=android.os.Build.VERSION.SDK_INT>=29 ? stateList.getStateDrawable(0) : context.getDrawable(R.drawable.ic_checkbox_def);
        stateList.setBounds(0,0,dr.getIntrinsicWidth(),dr.getIntrinsicHeight());
        setCompoundDrawables(null,null,stateList,null);
        setState(state);
    }


    public State getState(){return this.state;}
    public void setState(int state){
        switch (state){
            case -1: setState(State.Off); break;
            case 1: setState(State.On); break;
            default: setState(State.Default); break;
        }
    }
    public void setState(State state){
        this.state=state;
        refreshDrawableState();
    }


    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] states=super.onCreateDrawableState(extraSpace+1);
        mergeDrawableStates(states,State.getState(state));
        return states;
    }
}

