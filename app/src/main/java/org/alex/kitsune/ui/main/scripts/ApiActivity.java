package org.alex.kitsune.ui.main.scripts;

import android.os.Bundle;
import android.view.Gravity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.Nullable;
import org.alex.kitsune.Activity;
import androidx.appcompat.widget.Toolbar;
import org.alex.kitsune.R;
import org.alex.kitsune.utils.NetworkUtils;
import java.io.IOException;

import static org.alex.kitsune.utils.NetworkUtils.*;

public class ApiActivity extends Activity {
    Toolbar toolbar;
    WebView web;
    @Override public int getAnimationGravityIn(){return Gravity.END;}
    @Override public int getAnimationGravityOut(){return Gravity.START;}
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api);
        toolbar=findViewById(R.id.toolbar);
        initActionBar(toolbar);
        toolbar.setTitle("API");
        web=findViewById(R.id.web_view);
        web.getSettings().setJavaScriptEnabled(true);
        web.setWebViewClient(new WebViewClient());
        new Thread(()->{
            try {
                String answer=NetworkUtils.getString("https://raw.githubusercontent.com/alex-bayir/Kitsune/master/content/Scripts%20API.html",getHeadersDefault("github.com",null));
                NetworkUtils.getMainHandler().post(()-> web.loadDataWithBaseURL(null,answer,"text/html", "UTF-8",null));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
