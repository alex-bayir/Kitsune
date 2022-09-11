package com.alex.colorwheel;


import android.content.Context;
import androidx.preference.Preference;
import android.util.AttributeSet;

public class ColorChooserPreference extends Preference implements Preference.SummaryProvider<ColorChooserPreference> {
    public ColorChooserPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public ColorChooserPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ColorChooserPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorChooserPreference(Context context) {
        super(context);
        init();
    }

    private void init(){
        setSummaryProvider(this);
    }

    @Override
    protected void onClick() {
        new ChangeColorDialog(getSharedPreferences().getInt(getKey(),0), getContext(), color -> {
            getSharedPreferences().edit().putInt(getKey(),color).apply();
            callChangeListener(color);
            notifyChanged();
        }).show();
    }

    @Override
    public CharSequence provideSummary(ColorChooserPreference preference) {
        return "#"+String.format("%08X",preference.getSharedPreferences().getInt(getKey(),0));
    }
}
