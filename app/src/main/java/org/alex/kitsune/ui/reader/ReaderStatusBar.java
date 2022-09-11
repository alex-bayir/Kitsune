package org.alex.kitsune.ui.reader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;
import org.alex.kitsune.R;
import java.util.Calendar;

public final class ReaderStatusBar extends LinearLayout {
    private final BroadcastReceiver mBatteryReceiver;
    private final ImageView mImageViewBattery;
    private boolean mIsActive;
    private final TextView mTextViewBattery;
    private final TextView mTextViewClock;
    private final Runnable mTicker;
    private final Calendar mTime;

    public ReaderStatusBar(Context context) {
        this(context, null, 0);
    }

    public ReaderStatusBar(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ReaderStatusBar(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mIsActive=false;
        this.mBatteryReceiver=new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                ReaderStatusBar.this.update(intent);
            }
        };
        this.mTicker = new Runnable() {
            public void run() {
                ReaderStatusBar.this.onTimeChanged();
                long uptimeMillis = SystemClock.uptimeMillis();
                ReaderStatusBar.this.getHandler().postAtTime(ReaderStatusBar.this.mTicker, uptimeMillis + (60000 - (uptimeMillis % 60000)));
            }
        };
        View.inflate(context, R.layout.view_status, this);
        setOrientation(HORIZONTAL);
        this.mTextViewBattery=findViewById(R.id.textView_battery);
        this.mTextViewClock=findViewById(R.id.textView_clock);
        this.mImageViewBattery=findViewById(R.id.imageView_battery);
        this.mTime=Calendar.getInstance();
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateBattery();
        this.mTicker.run();
    }


    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getHandler().removeCallbacks(this.mTicker);
        getContext().unregisterReceiver(this.mBatteryReceiver);
    }

    private void updateBattery() {
        Intent registerReceiver = getContext().registerReceiver(this.mBatteryReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        if (registerReceiver != null) {
            update(registerReceiver);
        }
    }


    private void update(Intent intent) {
        int status = intent.getIntExtra(NotificationCompat.CATEGORY_STATUS, -1);
        boolean z = status == 2 || status == 5;
        int level = (intent.getIntExtra("level", -1) * 100) / intent.getIntExtra("scale", -1);
        this.mTextViewBattery.setText(level + "%");
        this.mImageViewBattery.setImageResource(z ? level >= 90 ? R.drawable.ic_battery_charging_90_black : level >= 50 ? R.drawable.ic_battery_charging_60_black : level >= 30 ? R.drawable.ic_battery_charging_40_black : R.drawable.ic_battery_charging_20_black : level >= 90 ? R.drawable.ic_battery_80_black : level >= 70 ? R.drawable.ic_battery_60_black : level >= 35 ? R.drawable.ic_battery_40_black : level >= 15 ? R.drawable.ic_battery_20_black : R.drawable.ic_battery_alert_black);
    }


    private void onTimeChanged(){
        if(isActive()){
            this.mTime.setTimeInMillis(System.currentTimeMillis());
            this.mTextViewClock.setText(DateFormat.format("k:mm", this.mTime));
        }
    }

    private boolean isActive(){return (this.mIsActive && (getVisibility()==VISIBLE));}

    public void show(){
        if(this.mIsActive){
            setVisibility(VISIBLE);
            onTimeChanged();
            //AnimationUtils.setVisibility(this, 0);
        }
    }

    public void hide(){
        setVisibility(GONE);
        //AnimationUtils.setVisibility(this, 8);
    }

    public void setIsActive(boolean z){
        this.mIsActive=z;
        if(!this.mIsActive){hide();}
    }
}

