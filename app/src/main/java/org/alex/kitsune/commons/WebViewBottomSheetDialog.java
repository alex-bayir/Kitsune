package org.alex.kitsune.commons;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import org.alex.kitsune.R;
import org.jetbrains.annotations.NotNull;

public class WebViewBottomSheetDialog extends BottomSheetDialogFragment {
    String url;
    WebView web;
    Callback<WebView> callback;
    public WebViewBottomSheetDialog(){}
    public WebViewBottomSheetDialog(Callback<WebView> onCreate){
        this.callback=onCreate;
    }
    public void setUrl(String url){
        this.url=url;
        if(web!=null){web.loadUrl(url);}
    }
    public String getUrl(){return url;}
    public void reload(){if(web!=null){web.reload();}}
    public void setCallback(Callback<WebView> callback){this.callback=callback;}
    public WebView getWeb(){return web;}
    public WebSettings getSettings(){return web!=null?web.getSettings():null;}
    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(R.layout.dialog_web, viewGroup, false);
    }
    @Override
    public void onViewCreated(@NotNull View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        web=(WebView) view;
        web.setOnKeyListener((v, keyCode, event) -> {
            if(keyCode== KeyEvent.KEYCODE_BACK && web!=null && web.canGoBack() && isVisible()){
                web.goBack();
                return true;
            }
            return false;
        });
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setLoadWithOverviewMode(true);
        web.getSettings().setUseWideViewPort(false);
        web.getSettings().setAllowContentAccess(true);
        web.getSettings().setAllowFileAccess(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setSaveFormData(true);
        web.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setDisplayZoomControls(false);
        web.setBackgroundColor(0);
        if(callback!=null){callback.call(web);}
    }

    private Runnable close_listener;
    public void setOnCloseListener(Runnable close_listener){
        this.close_listener=close_listener;
    }
    @Override
    public void onDismiss(@NonNull @NotNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if(close_listener!=null){close_listener.run();}
    }
}
