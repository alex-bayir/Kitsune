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
import org.alex.json.JSON;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.commons.HttpStatusException;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.services.LoadService;
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
    public static final OkHttpClient sHttpClient=new OkHttpClient.Builder().cookieJar(new CookieJar() {
        @Override
        public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {
            cookies.put(httpUrl.host(),list);
        }
        @NotNull
        @Override
        public List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {
            List<Cookie> list=Catalogs.getCookies(httpUrl.host(),cookies.get(httpUrl.host()));
            return list!=null ? list : new LinkedList<>();
        }
    }).readTimeout(60,TimeUnit.SECONDS).build();
    private static void printCookies(List<Cookie> cookies){if(cookies!=null)for(Cookie cookie:cookies){android.util.Log.e("Cookie",cookie.toString());}}
    public static void updateCookies(String domain,List<Cookie> cookies){
        NetworkUtils.cookies.put(domain,cookies);
    }
    public static Headers getHeadersDefault(String url){
        return getHeadersDefault(getDomain(null,url),url);
    }
    public static Headers getHeadersDefault(String domain,String url){
        return new Headers.Builder().add(HEADER_USER_AGENT, USER_AGENT_DEFAULT).add(HEADER_REFERER,getDomain(domain,url)).build();
    }
    public static String getDomain(String domain,String url){return domain!=null ? domain : url.substring(url.indexOf('/')+2,url.indexOf('/',8));}
    public static String getString(String url) throws IOException {
        return getString(url, null);
    }
    public static String getString(String url, okhttp3.Headers headers) throws IOException {
        String answer;
        Request request=new Request.Builder().url(url).headers(headers!=null ? headers : HEADERS_DEFAULT).cacheControl(CACHE_CONTROL_DEFAULT).get().build();
        Response response=sHttpClient.newCall(request).execute();
        try{answer=response.body().string();}catch (NullPointerException e){throw new IOException("ResponseBody is null");}
        response.close();
        if(response.code()!=200){throw new HttpStatusException(response.code(), url);}
    return answer;}
    public static String getString(String url, okhttp3.Headers headers,RequestBody body) throws IOException {
        String answer;
        Request request=new Request.Builder().url(url).headers(headers!=null ? headers : HEADERS_DEFAULT).cacheControl(CACHE_CONTROL_DEFAULT).post(body).build();
        Logs.e(bodyToString(request.body()));
        Response response=sHttpClient.newCall(request).execute();
        try{answer=response.body().string();}catch (NullPointerException e){throw new IOException("ResponseBody is null");}
        response.close();
        if(response.code()!=200){throw new HttpStatusException(response.code(), url);}
        return answer;}
    public static JSON.Object getJSONObject(String url) throws IOException {
        return JSON.Object.create(getString(url));
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
        if (isNotMetered(activeNetworkInfo)) {
            return true;
        }
        return false;
    }

    private static boolean isNotMetered(NetworkInfo networkInfo) {
        if (networkInfo.isRoaming()) {
            return false;
        }
        int type = networkInfo.getType();
        if (type == ConnectivityManager.TYPE_MOBILE ||type == ConnectivityManager.TYPE_WIFI || type == ConnectivityManager.TYPE_WIMAX || type == ConnectivityManager.TYPE_ETHERNET) {
            return true;
        }
        return false;
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
    public static String bodyToString(final RequestBody body){
        try {
            final Buffer buffer = new Buffer();
            body.writeTo(buffer);
            return buffer.readUtf8();
        } catch (final IOException e) {
            return "did not work";
        }
    }

    private static final Handler main=new Handler(Looper.getMainLooper());
    public static Handler getMainHandler(){return main;}

    public interface Callback2<K,V>{
        void call(K k,V v);
    }
    public static boolean load(String url, String domain, File file){
        return load(url,domain,file,null);
    }
    public static boolean load(String url, String domain, File file, LoadService.Task task){
        return load(url,domain,file,task,null);
    }
    public static boolean load(String url, String domain, File file, LoadService.Task task, Callback2<Long,Long> listener){
        return load(url,domain,file,task,listener,null,false);
    }
    public static boolean load(String url, String domain, File file, LoadService.Task task, Callback2<Long,Long> listener, Callback<Throwable> onBreak, boolean withSkip){
        InputStream in=null;
        OutputStream out=null;
        try{
            Response response=sHttpClient.newCall(new Request.Builder().url(url).headers(getHeadersDefault(domain,url)).get().build()).execute();
            if(response.isSuccessful() && response.body()!=null){
                long length=response.body().contentLength();
                long downloadedSize=(!file.getParentFile().mkdirs() && !file.createNewFile()) ? file.length() : 0;
                if(downloadedSize==length){return true;}
                out=new FileOutputStream(file,withSkip);
                in=response.body().byteStream();
                byte[] arr=new byte[0x400]; long i=0; int read;
                if(withSkip){i=in.skip(downloadedSize);}
                if(task==null){
                    while((read=in.read(arr))>0){
                        out.write(arr,0,read);
                        if(listener!=null){i+=read; listener.call(i,length);}
                    }
                }else{
                    while((read=in.read(arr))>0 && !task.isCanceled()){
                        out.write(arr,0,read);
                        if(listener!=null){i+=read; listener.call(i,length);}
                    }
                }
                out.close();
                in.close();
                if(task!=null && task.isCanceled()){return false;}
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
