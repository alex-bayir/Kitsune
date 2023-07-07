package org.alex.kitsune.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import okhttp3.*;
import okio.Buffer;
import com.alex.json.java.JSON;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.commons.Callback2;
import org.alex.kitsune.commons.HttpStatusException;
import org.alex.kitsune.ui.shelf.Catalogs;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class NetworkUtils {

    public static final String HEADER_REFERER = "Referer";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String USER_AGENT_DEFAULT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 YaBrowser/22.11.5.715 Yowser/2.5 Safari/537.36";
    public static final Headers HEADERS_DEFAULT = new Headers.Builder().add(HEADER_USER_AGENT, USER_AGENT_DEFAULT).build();
    private static final CacheControl CACHE_CONTROL_DEFAULT = new CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build();
    private static final TreeMap<String,List<Cookie>> cookies=new TreeMap<>();
    public static OkHttpClient sHttpClient=new OkHttpClient.Builder().cookieJar(new CookieJar() {
        @Override
        public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {
            cookies.put(httpUrl.host(),list);
        }
        @NotNull
        @Override
        public List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {
            List<Cookie> list=getCookies(httpUrl.host());
            return list!=null ? list : new LinkedList<>();
        }
    }).readTimeout(15,TimeUnit.SECONDS).build();
    public static void setNewTimeout(long timeout){
        sHttpClient=sHttpClient.newBuilder()
                .connectTimeout(timeout/2,TimeUnit.MILLISECONDS)
                .readTimeout(timeout/2,TimeUnit.MILLISECONDS)
                .writeTimeout(timeout/2,TimeUnit.MILLISECONDS)
                .build();
    }
    public static void setTimeout(android.content.SharedPreferences prefs){
        setNewTimeout(prefs.getInt("Timeout",15)*1000L);
    }
    public static final OkHttpClient scrambledClient=sHttpClient.newBuilder().addInterceptor(new ScrambledInterceptor()).build();
    public static OkHttpClient getClient(boolean descramble){
        return descramble ? scrambledClient : sHttpClient;
    }
    private static void printCookies(List<Cookie> cookies){if(cookies!=null)for(Cookie cookie:cookies){android.util.Log.e("Cookie",cookie.toString());}}
    public static List<Cookie> getCookies(String domain){
        return Catalogs.getCookies(domain,cookies.get(domain));
    }
    public static void updateCookies(String domain,List<Cookie> cookies){
        NetworkUtils.cookies.put(domain,cookies);
    }
    public static Headers getHeadersDefault(String url){
        return getHeadersDefault(getDomain(null,url),url);
    }
    public static Headers getHeadersDefault(String domain,String url){
        return new Headers.Builder().add(HEADER_USER_AGENT, USER_AGENT_DEFAULT).add(HEADER_REFERER,"https://"+getDomain(domain,url)).add("Cookie",cookies(getDomain(domain,url))).build();
    }
    public static String cookies(String domain){
        if(domain!=null && getCookies(domain)!=null){
            StringBuilder cookies=new StringBuilder();
            for(Cookie cookie:getCookies(domain)){
                if(cookies.length()>0){cookies.append("; ");}
                cookies.append(cookie);
            }
            return cookies.toString();
        }else{
            return "";
        }
    }
    public static String getDomain(String domain,String url){return domain!=null ? domain : url.substring(url.indexOf('/')+2,url.indexOf('/',8));}
    public static String getString(String url) throws IOException {
        return getString(url, null);
    }
    public static String getString(String url, okhttp3.Headers headers) throws IOException {
        String answer;
        Request request=new Request.Builder().url(url).headers(headers!=null ? headers : HEADERS_DEFAULT).cacheControl(CACHE_CONTROL_DEFAULT).get().build();
        Response response=sHttpClient.newCall(request).execute();
        answer=response.body()==null ? null:response.body().string();
        response.close();
        if(response.code()!=200){throw new HttpStatusException(response.code(), url);}
    return answer;}
    public static String getString(String url, okhttp3.Headers headers,RequestBody body) throws IOException {
        String answer;
        Request request=new Request.Builder().url(url).headers(headers!=null ? headers : HEADERS_DEFAULT).cacheControl(CACHE_CONTROL_DEFAULT).post(body).build();
        //System.out.println(toString(request));
        Response response=sHttpClient.newCall(request).execute();
        answer=response.body()==null ? null:response.body().string();
        response.close();
        if(response.code()!=200){throw new HttpStatusException(response.code(), url);}
        return answer;}
    public static JSON getJSON(String url) throws IOException {
        return JSON.json(getString(url));
    }

    public static Document getDocument(String url) throws IOException {
        return getDocument(url, null);
    }

    public static Document getDocument(String str, Headers headers) throws IOException {
        return Jsoup.parse(getString(str, headers), str);
    }
    public static Document getDocument(String str, Headers headers,RequestBody body) throws IOException {
        return Jsoup.parse(getString(str, headers,body), str);
    }
    public static boolean isNetworkAvailable(Context context) {
        NetworkInfo activeNetworkInfo;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null || (activeNetworkInfo = connectivityManager.getActiveNetworkInfo()) == null || !activeNetworkInfo.isConnected()) {
            return false;
        }
        return isNotMetered(activeNetworkInfo);
    }

    private static boolean isNotMetered(NetworkInfo networkInfo) {
        if (networkInfo.isRoaming()) {
            return false;
        }
        int type = networkInfo.getType();
        return type == ConnectivityManager.TYPE_MOBILE || type == ConnectivityManager.TYPE_WIFI || type == ConnectivityManager.TYPE_WIMAX || type == ConnectivityManager.TYPE_ETHERNET;
    }

    public static GlideUrl getGlideUrl(String url,String domain){
        return new GlideUrl(url,new LazyHeaders.Builder()
                .setHeader(HEADER_REFERER,getDomain(domain,url))
                .setHeader(HEADER_USER_AGENT, USER_AGENT_DEFAULT)
                .build()
        );
    }

    public static Headers extendHeaders(Map<String,String> headers){
        if(headers==null){return NetworkUtils.HEADERS_DEFAULT;}
        Headers.Builder builder=NetworkUtils.HEADERS_DEFAULT.newBuilder();
        headers.forEach(builder::add);
        return builder.build();
    }
    public static RequestBody convertBody(Map<String,String> body){
        if(body==null){return null;}
        FormBody.Builder builder=new FormBody.Builder();
        body.forEach(builder::add);
        return builder.build();
    }
    public static String toString(Request request){
        String str=request.toString();
        if(request.body()!=null){
            try {
                final Buffer buffer = new Buffer();
                request.body().writeTo(buffer);
                str+=",body:"+buffer.readUtf8();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return str;
    }

    private static final Handler main=new Handler(Looper.getMainLooper());
    public static Handler getMainHandler(){return main;}
    public static boolean load(String url, String domain, File file){
        return load(url,domain,file,null);
    }
    public static boolean load(String url, String domain, File file, Boolean task){
        return load(url,domain,file,task,null);
    }
    public static boolean load(String url, String domain, File file, Boolean task, Callback2<Long,Long> listener){
        return load(url,domain,file,task,listener,null,false);
    }
    public static boolean load(String url, String domain, File file, Boolean cancel_flag, Callback2<Long,Long> listener, Callback<Throwable> onBreak, boolean withSkip){
        return load(sHttpClient,url,domain,file,cancel_flag,listener,onBreak,withSkip);
    }
    public static boolean load(OkHttpClient client,String url, String domain, File file, Boolean cancel_flag, Callback2<Long,Long> listener, Callback<Throwable> onBreak, boolean withSkip){
        InputStream in=null;
        OutputStream out=null;
        try{
            Response response=client.newCall(new Request.Builder().url(url).headers(getHeadersDefault(domain,url)).get().build()).execute();
            if(response.isSuccessful() && response.body()!=null){
                long length=response.body().contentLength();
                long downloadedSize=(!file.getParentFile().mkdirs() && !file.createNewFile()) ? file.length() : 0;
                if(downloadedSize==length){return true;}
                out=new FileOutputStream(file,withSkip);
                in=response.body().byteStream();
                byte[] arr=new byte[0x400]; long i=0; int read;
                if(withSkip){i=in.skip(downloadedSize);}
                if(cancel_flag==null){
                    while((read=in.read(arr))>0){
                        out.write(arr,0,read);
                        if(listener!=null){i+=read; listener.call(i,length);}
                    }
                }else{
                    while((read=in.read(arr))>0 && !cancel_flag){
                        out.write(arr,0,read);
                        if(listener!=null){i+=read; listener.call(i,length);}
                    }
                }
                out.close();
                in.close();
                if(cancel_flag!=null && cancel_flag){return false;}
            }else{throw new HttpStatusException(response.code(),response.request().url().toString());}
        }catch(Exception e){
            e.printStackTrace();
            try{if(in!=null){in.close();} if(out!=null){out.close();}}catch(IOException e2){e2.printStackTrace();}
            file.delete();
            if(onBreak!=null){onBreak.call(e);}
            return false;
        }
        return true;
    }
}
