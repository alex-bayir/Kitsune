package org.alex.kitsune.commons;

public class RadioGroup<T>{
    T currentChecked;
    T[] values;
    final OnChangeListener<T> listener;
    interface OnChangeListener<T>{
        void onChange(T last,T current);
    }
    public RadioGroup(OnChangeListener<T> listener,T[] values){
        this.listener=listener;
        init(values);
    }
    public RadioGroup(OnChangeListener<T> listener){
        this.listener=listener;
    }
    public void init(T[] values){
        this.values=values;
        this.currentChecked=values[0];
    }
    public T getCurrentChecked(){return currentChecked;}
    public void setCurrentChecked(int index){
        listener.onChange(currentChecked,(currentChecked=values[index]));
    }
    public void setCurrentChecked(T t){
        for(int i=0;i<values.length;i++){if(t==values[i]){setCurrentChecked(i); return;}}
    }
}
