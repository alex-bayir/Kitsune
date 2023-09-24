package com.alex.threestates;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import com.google.android.material.checkbox.MaterialCheckBox;

public class CheckBox3 extends MaterialCheckBox {
    private State state=State.Default;
    public CheckBox3(Context context){this(context,null);}

    public CheckBox3(Context context, @Nullable AttributeSet attrs){
        this(context, attrs,com.google.android.material.R.attr.checkboxStyle);
    }

    public CheckBox3(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    public void init(Context context){
        setButtonDrawable(State.getStateDrawable(context));
        setUseMaterialThemeColors(false);
    }

    public State getState(){return state;}

    public void setState(int state){
        setState(State.valueOf(state));
    }
    public void setState(State state){
        if(this.state!=state){
            this.state=state;
            refreshDrawableState();
        }
    }

    @Override
    public void setChecked(boolean checked) {
        toggle();
    }

    @Override
    public void toggle() {
        setState(state!=null?state.next():State.Default);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] states=super.onCreateDrawableState(extraSpace+1);
        mergeDrawableStates(states,State.getState(state));
        return states;
    }

}
