package org.alex.kitsune.commons;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

public class ListPreference  extends Preference implements Preference.SummaryProvider<ListPreference> {
    Resources res;
    private CharSequence[] entries;
    private Object[] entryValues;
    private int defValueInt;
    private String defValueString;

    public ListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    public ListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public ListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs){
        setSummaryProvider(this);
        res=getContext().getResources();
        int size=attrs.getAttributeCount();
        for(int i=0;i<size;i++){
            switch (attrs.getAttributeName(i)){
                case "entries":
                    entries=res.getTextArray(attrs.getAttributeResourceValue(i,-1));
                    break;
                case "entryValues":
                    entryValues=res.getTextArray(attrs.getAttributeResourceValue(i,-1));
                    if(entryValues.length==0 || entryValues[0]==null){
                        entryValues=translate(res.getIntArray(attrs.getAttributeResourceValue(i,-1)));
                    }
                    break;
                case "defaultValue":
                    defValueInt=attrs.getAttributeIntValue(i,Integer.MIN_VALUE);
                    defValueString=attrs.getAttributeValue(i);
                    break;
            }

        }
    }

    @Override
    protected void onClick() {
        new AlertDialog.Builder(getContext())
                .setTitle(getTitle())
                .setSingleChoiceItems(entries, getIndex(entryValues,getValue(getKey(),getDefaultValueInt(),getDefValueString())), (dialog, which) -> {
                    saveValue(getKey(),entryValues[which]);
                    notifyChanged();
                    callChangeListener(entryValues[which]);
                    dialog.cancel();
                }).setNegativeButton(android.R.string.cancel,null)
                .create().show();
    }

    public CharSequence[] getEntries(){return entries;}
    public Object[] getEntryValues(){return entryValues;}
    public int getDefaultValueInt(){return defValueInt;}
    public String getDefValueString(){return defValueString;}

    private Object getValue(String key, Integer defInt, String defString){
        try{Object obj=getValue(key,defInt); return obj.equals(Integer.MIN_VALUE) ? getValue(key,defString) : obj;}catch (Exception e){return getValue(key,defString);}
    }

    public String getValue(String key,String def){return getSharedPreferences().getString(key,def);}
    public int getValue(String key,int def){return getSharedPreferences().getInt(key,def);}
    public static int getIndex(Object[] arr, Object obj){
        int index=-1;
        for(int i=0;i<arr.length;i++){if(arr[i].equals(obj)){index=i; break;}}
    return index;}

    private void saveValue(String key, Object obj){
        if(obj instanceof Integer){
            getSharedPreferences().edit().putInt(key,(int)obj).apply();
        }else{
            getSharedPreferences().edit().putString(key,obj.toString()).apply();
        }
    }

    public static Integer[] translate(int[] arr){
        Integer[] a=new Integer[arr.length];
        for(int i=0;i<a.length;i++){a[i]=arr[i];}
    return a;}

    public String getDefaultEntry(){
        Object def=defValueInt==Integer.MIN_VALUE ? defValueString : defValueInt;
        for(int i=0; i<entryValues.length;i++){
            if(entryValues[i].equals(def)){return entries[i].toString();}
        }
        return def.toString();
    }

    @Override
    public CharSequence provideSummary(ListPreference listPreference) {
        int index=getIndex(listPreference.getEntryValues(), getValue(getKey(),listPreference.getDefaultValueInt(),listPreference.getDefValueString()));
        return index<0 ? getDefaultEntry() : listPreference.getEntries()[index].toString();
    }
}
