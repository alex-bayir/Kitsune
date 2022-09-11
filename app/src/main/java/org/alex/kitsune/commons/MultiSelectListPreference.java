package org.alex.kitsune.commons;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import org.json.JSONArray;
import java.util.HashSet;
import java.util.Hashtable;

public class MultiSelectListPreference extends Preference implements Preference.SummaryProvider<MultiSelectListPreference>{
    private CharSequence[] entries;
    private boolean[] entryValues;
    public MultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setSummaryProvider(this);
    }

    public MultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSummaryProvider(this);
    }

    public MultiSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSummaryProvider(this);
    }

    public MultiSelectListPreference(Context context) {
        super(context);
        setSummaryProvider(this);
    }
    public static void save(SharedPreferences sharedPreferences,String key,HashSet<String> values){
        JSONArray arr=new JSONArray();
        for(String tmp:values){arr.put(tmp);}
        sharedPreferences.edit().putString(key,arr.toString()).apply();
    }
    public void save(){
        JSONArray arr=new JSONArray();
        for(int i=0;i<entryValues.length;i++){
            if(entryValues[i]){
                arr.put(entries[i]);
            }
        }
        getSharedPreferences().edit().putString(getKey(),arr.toString()).apply();
    }
    public void setEntries(HashSet<String> hashSet){
        setEntries(hashSet,true);
    }
    public void setEntries(HashSet<String> hashSet, boolean restore){
        Hashtable<String,Boolean> hashtable=new Hashtable<>();
        if(hashSet!=null){
            for(String str:hashSet){
                hashtable.put(str,false);
            }
        }
        if(restore){
            for(String key:getEntries()){
                hashtable.put(key, hashtable.containsKey(key));
            }
        }else{
            for(String key:getEntries()){
                if(hashtable.containsKey(key)){
                    hashtable.put(key, true);
                }
            }
        }

        entries=hashtable.keySet().toArray(new String[0]);
        entryValues=convert(hashtable.values().toArray(new Boolean[0]));
    }
    private static boolean[] convert(Boolean[] arr){
        boolean[] a=new boolean[arr.length];
        for(int i=0;i<a.length;i++){a[i]=arr[i];}
        return a;
    }
    public static HashSet<String> getEntries(SharedPreferences sharedPreferences, String key, String defValue){
        HashSet<String> s=new HashSet<>();
        String tmp=sharedPreferences.getString(key,null);
        if(tmp!=null){
            try{
                JSONArray arr=new JSONArray(tmp);
                for(int i=0;i<arr.length();i++){
                    s.add(arr.getString(i));
                }
                return s;
            } catch (Exception e) {
                e.printStackTrace();
                if(defValue!=null){s.add(defValue);}
                return s;
            }
        }else{
            if(defValue!=null){s.add(defValue);}
            return s;
        }
    }
    private HashSet<String> getEntries(){
        return getEntries(getSharedPreferences(),getKey(),null);
    }


    @Override
    protected void onClick() {
        new AlertDialog.Builder(getContext())
                .setTitle(getTitle())
                .setMultiChoiceItems(entries, entryValues, (dialog, which, isChecked) -> {
                    entryValues[which]=isChecked;
                    save();
                    notifyChanged();
                })
                .setNegativeButton("CLOSE",null)
                .create().show();
    }

    @Override
    public CharSequence provideSummary(MultiSelectListPreference preference) {
        StringBuilder tmp=new StringBuilder(); int k=0;
        for(int i=0;i<preference.entries.length;i++){
            if(preference.entryValues[i]){
                if(k++>0){tmp.append(", ");}
                tmp.append(preference.entries[i]);
            }
        }
        return (k==preference.entries.length ? "All" : (tmp.length()>0 ? tmp : "Nothing selected"));
    }
}
