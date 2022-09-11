package com.alex.threestates;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import org.alex.threestates.R;
import com.google.android.material.checkbox.MaterialCheckBox;

public class CheckBox3 extends MaterialCheckBox {
    private State state=State.Default;
    public CheckBox3(Context context){this(context,null);}

    public CheckBox3(Context context, @Nullable AttributeSet attrs){
        this(context, attrs,com.google.android.material.R.attr.checkboxStyle);
    }

    public CheckBox3(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }
    public void init(AttributeSet attrs){
        StateListDrawable d=(StateListDrawable) getContext().obtainStyledAttributes(attrs, R.styleable.CheckBox3).getDrawable(0);
        setButtonDrawable(d);
        setOnCheckedChangeListener((buttonView, isChecked) -> setState(getState().next()));
        setState(State.Default);
        setUseMaterialThemeColors(false);
    }


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
    public State getState(){return state;}

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] states=super.onCreateDrawableState(extraSpace+1);
        mergeDrawableStates(states,State.getState(state));
        return states;
    }

}
