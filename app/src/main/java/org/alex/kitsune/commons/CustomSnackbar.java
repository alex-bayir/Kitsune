package org.alex.kitsune.commons;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import org.alex.kitsune.R;

public class CustomSnackbar extends BaseTransientBottomBar<CustomSnackbar> {
    ImageView icon;
    TextView text;
    Button button;
    private CustomSnackbar(ViewGroup parent, View content, ContentViewCallback contentViewCallback){
        super(parent,content,contentViewCallback);
        icon=content.findViewById(R.id.image);
        text=content.findViewById(R.id.text);
        button=content.findViewById(R.id.button);
    }
    private CustomSnackbar(ViewGroup parent, View content){
        this(parent,content,new ContentViewCallback(parent));
    }
    public static CustomSnackbar makeSnackbar(ViewGroup parent, @Duration int duration){
        return makeSnackbar(parent,duration, R.drawable.bg_snackbar);
    }
    public static CustomSnackbar makeSnackbar(ViewGroup parent, @Duration int duration, int background){
        return new CustomSnackbar(parent,LayoutInflater.from(parent.getContext()).inflate(R.layout.snackbar_update,parent,false)).setBackground(background).setDuration(duration);
    }
    public static CustomSnackbar makeSnackbar(ViewGroup parent, @Duration int duration, Drawable background){
        return new CustomSnackbar(parent,LayoutInflater.from(parent.getContext()).inflate(R.layout.snackbar_update,parent,false)).setBackground(background).setDuration(duration);
    }

    public CustomSnackbar setText(CharSequence text){
        this.text.setText(text); return this;
    }
    public CustomSnackbar setText(int string){
        return setText(getContext().getString(string));
    }
    public CustomSnackbar setIcon(int drawable){
        return setIcon(getContext().getDrawable(drawable));
    }
    public CustomSnackbar setIcon(Drawable icon){
        this.icon.setImageDrawable(icon);
        this.icon.setVisibility(View.VISIBLE);
        return this;
    }
    public CustomSnackbar setAction(int string, final View.OnClickListener listener){
        return setAction(getContext().getString(string),listener);
    }
    public CustomSnackbar setAction(CharSequence text, final View.OnClickListener listener){
        button.setText(text);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(listener);
        return this;
    }
    public ImageView getIcon() {
        return icon;
    }
    public TextView getText() {
        return text;
    }
    public Button getButton() {
        return button;
    }

    public CustomSnackbar setBackground(int drawable){
        return setBackground(getContext().getDrawable(drawable));
    }
    public CustomSnackbar setBackground(Drawable drawable){
        getView().setBackground(drawable); return this;
    }
    public CustomSnackbar setBackgroundAlpha(int alpha){
        getView().getBackground().setAlpha(alpha); return this;
    }
    public CustomSnackbar setGravity(int gravity){
        if(getView().getLayoutParams() instanceof CoordinatorLayout.LayoutParams params){
            params.gravity=gravity;
            getView().setLayoutParams(params);
        }else if(getView().getLayoutParams() instanceof FrameLayout.LayoutParams params){
            params.gravity=gravity;
            getView().setLayoutParams(params);
        }
        return this;
    }
    public CustomSnackbar setMargins(int left, int top, int right, int bottom){
        if(getView().getLayoutParams() instanceof ViewGroup.MarginLayoutParams params){
            params.setMargins(left, top, right, bottom);
            params.setMarginStart(left);
            params.setMarginEnd(right);
            getView().setLayoutParams(params);
        }
        return this;
    }
    public CustomSnackbar setPadding(int padding){
        return setPadding(padding,padding,padding,padding);
    }
    public CustomSnackbar setPadding(int left, int top, int right, int bottom){
        getView().setPadding(left, top, right, bottom);
        return this;
    }

    private static class ContentViewCallback implements com.google.android.material.snackbar.ContentViewCallback {

        private final View content;

        public ContentViewCallback(View content) {
            this.content = content;
        }

        @Override
        public void animateContentIn(int delay, int duration) {
            // add custom *in animations for your views
            // e.g. original snackbar uses alpha animation, from 0 to 1
            ViewCompat.setScaleY(content, 0f);
            ViewCompat.animate(content).scaleY(1f).setDuration(duration).setStartDelay(delay);
        }

        @Override
        public void animateContentOut(int delay, int duration) {
            // add custom *out animations for your views
            // e.g. original snackbar uses alpha animation, from 1 to 0
            ViewCompat.setScaleY(content, 1f);
            ViewCompat.animate(content).scaleY(0f).setDuration(duration).setStartDelay(delay);
        }
    }
}
