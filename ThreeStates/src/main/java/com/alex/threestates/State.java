package com.alex.threestates;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import org.alex.threestates.R;

public enum State{
    Default(0),
    On(1),
    Off(-1);
    public final int code;
    State(int code){this.code=code;}
    public int getCode(){return code;}
    public State next(){
        switch (this){
            case On: return Off;
            case Off: return Default;
            default: return On;
        }
    }
    public static int[] getState(State state){
        switch (state==null ? Default:state){
            case On: return new int[]{R.attr.state_on};
            case Off: return new int[]{R.attr.state_off};
            default: return new int[]{R.attr.state_def};
        }
    }
    public Drawable getStateDrawable(StateListDrawable d){
        return d!=null ? d.getStateDrawable(ordinal()) : null;
    }
}
