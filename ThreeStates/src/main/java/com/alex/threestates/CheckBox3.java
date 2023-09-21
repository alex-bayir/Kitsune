package com.alex.threestates;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.AttributeSet;
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

    public State getState(){return state;}

    public void setState(int state){
        setState(State.valueOf(state));
    }
    public void setState(State state){
        if(this.state!=state){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                if(getButtonDrawable() instanceof StateListDrawable state_list){
                    state_list.selectDrawable(state_list.findStateDrawableIndex(State.getState(state)));
                    if(state.getStateDrawable(state_list) instanceof Animatable animatable){
                        animatable.start();
                    }
                }
            }
            this.state=state;
        }
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] states=super.onCreateDrawableState(extraSpace+1);
        mergeDrawableStates(states,State.getState(state));
        return states;
    }

}
