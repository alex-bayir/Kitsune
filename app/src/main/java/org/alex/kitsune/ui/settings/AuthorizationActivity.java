package org.alex.kitsune.ui.settings;

import android.content.SharedPreferences;
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
import androidx.preference.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.manga.Manga_Scripted;
import org.alex.kitsune.scripts.Script;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.ui.shelf.Catalogs;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class AuthorizationActivity extends AppCompatActivity {
    String source;
    TextView user;
    WebViewDialogFragment dialog=new WebViewDialogFragment(obj -> user.setText(obj));
    SharedPreferences prefs;
    ArrayList<Catalogs.Container> containers;
    Catalogs.Container container;

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
        prefs=PreferenceManager.getDefaultSharedPreferences(this);
        containers=Catalogs.getCatalogs(prefs);
        for(Catalogs.Container c:containers){
            if(c.source.equals(source)){
                container=c; break;
            }
        }
        Script script=Manga_Scripted.getScript(source);
        if(script!=null){
            String domain=script.getString("provider",null);
            if(domain!=null){
                dialog.show(getSupportFragmentManager(),"");
                dialog.setUrl("https://" + domain);
                dialog.setCookies_callback(cookies->{
                    container.cookies=cookies;
                    prefs.edit().putString(Constants.source_order,Catalogs.Container.toJSON(containers).toString()).apply();
                });
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
    public static class WebViewDialogFragment extends BottomSheetDialogFragment {
        String url;
        WebView web;
        Callback<String> callback;
        Callback<String> cookies_callback;
        public WebViewDialogFragment(Callback<String> callback){
            this.callback=callback;
        }
        public void setUrl(String url){
            this.url=url;
            if(web!=null){web.loadUrl(url);}
        }
        public void reload(){if(web!=null){web.reload();}}
        public void setCallback(Callback<String> callback){
            this.callback=callback;
        }
        public void setCookies_callback(Callback<String> callback){
            this.cookies_callback=callback;
        }
        @Override
        public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
            return layoutInflater.inflate(R.layout.dialog_web, viewGroup, false);
        }
        @Override
        public void onViewCreated(@NotNull View view, Bundle bundle) {
            super.onViewCreated(view, bundle);
            web=(WebView) view;
            web.getSettings().setJavaScriptEnabled(true);
            web.getSettings().setSaveFormData(true);
            web.setWebViewClient(new WebViewClient(){
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    String cookies=android.os.Build.VERSION.SDK_INT >= 33? URLDecoder.decode(CookieManager.getInstance().getCookie(url), StandardCharsets.UTF_8) : URLDecoder.decode(CookieManager.getInstance().getCookie(url));
                    StringBuilder text=new StringBuilder(),save=new StringBuilder();
                    for(String cookie:cookies.split("; ")){
                        String[] nv=cookie.split("=",2);
                        try{nv[1]=new JSONObject(nv[1]).toString(2);}catch(JSONException ignored){}
                        if(nv[0].contains("user")||nv[0].contains("token")){
                            text.append(nv[0]).append(':').append(nv[1]).append('\n');
                            save.append("; ").append(cookie);
                        }
                    }
                    if(callback!=null){callback.call(text.toString());}
                    if(cookies_callback!=null){cookies_callback.call(save.length()==0?null:save.substring(2));}
                }
            });
            web.loadUrl(url);
        }
    }
}