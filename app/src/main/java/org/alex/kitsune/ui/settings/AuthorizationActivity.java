package org.alex.kitsune.ui.settings;

import android.view.*;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.WebViewBottomSheetDialog;
import org.alex.kitsune.manga.Manga_Scripted;
import org.alex.kitsune.scripts.Script;
import org.alex.kitsune.ui.shelf.Catalogs;

public class AuthorizationActivity extends AppCompatActivity {
    String source;
    TextView user;
    WebViewBottomSheetDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorization);
        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        source=getIntent().getStringExtra("source");
        toolbar.setTitle(source);
        user=findViewById(R.id.user_info);
        dialog=new WebViewBottomSheetDialog(web->{
            web.setWebViewClient(new WebViewClient(){
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    String cookie=CookieManager.getInstance().getCookie(url);
                    user.setText(Catalogs.updateCookies(web.getContext(),source,cookie)[1]);
                }
            });
            web.loadUrl(dialog.getUrl());
        });
        Script script=Manga_Scripted.getScript(source);
        if(script!=null){
            String domain=script.getString("provider",null);
            if(domain!=null){
                dialog.show(getSupportFragmentManager(),"");
                dialog.setUrl("https://" + domain);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_authorization_menu,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home: finish(); break;
            case R.id.show_web: if(!dialog.isVisible()){dialog.show(getSupportFragmentManager(),"");}else{dialog.reload();} break;
        }
        return super.onOptionsItemSelected(item);
    }
}