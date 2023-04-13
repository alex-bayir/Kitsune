package org.alex.kitsune.utils;

import android.os.Bundle;

public class Binder<T> extends android.os.Binder {
    T data;
    public Binder(T data){
        this.data=data;
    }
    public T getData(){
        return data;
    }
    public Bundle toBundle(String key,Bundle bundle){
        bundle.putBinder(key,this);
        return bundle;
    }
    public Bundle toBundle(String key){
        return toBundle(key,new Bundle());
    }
}
