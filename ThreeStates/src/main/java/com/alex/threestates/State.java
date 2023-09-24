package com.alex.threestates;

import android.content.Context;
import android.graphics.drawable.*;
import org.alex.threestates.R;

public enum State{
    Default(0),
    On(1),
    Off(2);
    public final int code;
    State(int code){this.code=code;}
    public int getCode(){return code;}
    public State next(){
        return switch (this) {
            case On -> Off;
            case Off -> Default;
            default -> On;
        };
    }
    public static State valueOf(int state){
        return switch (state){
            case -1,2 -> State.Off;
            case 1    -> State.On;
            default   -> State.Default;
        };
    }
    public int[] getState(){return getState(this);}
    public static int[] getState(State state){
        return switch (state == null ? Default : state) {
            case On  -> new int[]{R.attr.state_on };
            case Off -> new int[]{R.attr.state_off};
            default  -> new int[]{R.attr.state_def};
        };
    }

    public static AnimatedStateListDrawable getStateDrawable(Context context){
        AnimatedStateListDrawable drawable=new AnimatedStateListDrawable();
        AnimatedVectorDrawable def=(AnimatedVectorDrawable) context.getDrawable(R.drawable.def);
        AnimatedVectorDrawable on =(AnimatedVectorDrawable) context.getDrawable(R.drawable.on);
        AnimatedVectorDrawable off=(AnimatedVectorDrawable) context.getDrawable(R.drawable.off);
        drawable.addState(Default.getState(),def,R.id.def);
        drawable.addState(On.getState(),on,R.id.on);
        drawable.addState(Off.getState(),off,R.id.off);
        drawable.addTransition(R.id.off,R.id.def,def,true);
        drawable.addTransition(R.id.def,R.id.on,on,true);
        drawable.addTransition(R.id.on,R.id.off,off,true);
        drawable.setState(Default.getState());
        return drawable;
    }
}
