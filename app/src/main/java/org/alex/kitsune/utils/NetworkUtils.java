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
import org.alex.kitsune.commons.Callback2;
import org.alex.kitsune.commons.HttpStatusException;
import org.alex.kitsune.ui.shelf.Catalogs;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class NetworkUtils {

    public static final String HEADER_REFERER = "Referer";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String USER_AGENT_DEFAULT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 YaBrowser/22.11.5.715 Yowser/2.5 Safari/537.36";
    public static final Headers HEADERS_DEFAULT = new Headers.Builder().add(HEADER_USER_AGENT, USER_AGENT_DEFAULT).build();
    private static final CacheControl CACHE_CONTROL_DEFAULT = new CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build();
    private static final TreeMap<String,List<Cookie>> cookies=new TreeMap<>();
    private static OkHttpClient client=new OkHttpClient.Builder().cookieJar(new CookieJar() {
        private static final LinkedList<Cookie> empty_cookies=new LinkedList<>();
        @Override
        public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {
            cookies.put(httpUrl.host(),list);
        }
        @NotNull
        @Override
        public List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {
            return getCookies(httpUrl.host(),empty_cookies);
        }
    }).readTimeout(15,TimeUnit.SECONDS).build();
    public static OkHttpClient scramble_client=buildClient(client.newBuilder(),true);

    public static OkHttpClient buildClient(OkHttpClient.Builder client_builder,boolean descramble){
        return descramble? client_builder.addInterceptor(new ScrambledInterceptor()).build(): client_builder.build();
    }
    public static OkHttpClient getClient(boolean descramble){
        return descramble? scramble_client : client;
    }
    private static void update(OkHttpClient.Builder client_builder){
        client=buildClient(client_builder,false);
        scramble_client=buildClient(client_builder,true);
    }
    public static void setNewTimeout(long timeout){
        update(client.newBuilder()
                .connectTimeout(timeout/2,TimeUnit.MILLISECONDS)
                .readTimeout(timeout/2,TimeUnit.MILLISECONDS)
                .writeTimeout(timeout/2,TimeUnit.MILLISECONDS)
        );
    }
    public static void setTimeout(android.content.SharedPreferences prefs){
        setNewTimeout(prefs.getInt("Timeout",15)*1000L);
    }
    private static void printCookies(List<Cookie> cookies){if(cookies!=null)for(Cookie cookie:cookies){android.util.Log.e("Cookie",cookie.toString());}}
    public static List<Cookie> getCookies(String domain){
        return getCookies(domain,null);
    }
    public static List<Cookie> getCookies(String domain,List<Cookie> def){
        return Catalogs.getCookies(domain,cookies.getOrDefault(domain,def));
    }
    public static void updateCookies(String domain,List<Cookie> cookies){
        NetworkUtils.cookies.put(domain,cookies);
    }
    public static Headers getHeadersDefault(String url){
        return getHeadersDefault(getDomain(null,url),url);
    }
    public static Headers getHeadersDefault(String domain,String url){
        return getHeadersBuilder(domain,url).build();
    }
    public static Headers.Builder getHeadersBuilder(String domain,String url){
        return new Headers.Builder().add(HEADER_USER_AGENT, USER_AGENT_DEFAULT).add(HEADER_REFERER,"https://"+getDomain(domain,url)).add("Cookie",cookies(getDomain(domain,url)));
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
    public static String getDomain(String domain,String url){return domain!=null ? domain : Utils.group(url,"https?:[\\/][\\/]([^\\/]+)",null);}
    public static String getString(String url) throws IOException {
        return getString(url, null);
    }
    public static String getString(String url, okhttp3.Headers headers) throws IOException {
        String answer;
        Request request=new Request.Builder().url(url).headers(headers!=null ? headers : HEADERS_DEFAULT).cacheControl(CACHE_CONTROL_DEFAULT).get().build();
        Response response=client.newCall(request).execute();
        answer=response.body().string();
        response.close();
        if(response.code()!=200){throw new HttpStatusException(response.code(), url);}
    return answer;}
    public static String getString(String url, okhttp3.Headers headers,RequestBody body) throws IOException {
        String answer;
        Request request=new Request.Builder().url(url).headers(headers!=null ? headers : HEADERS_DEFAULT).cacheControl(CACHE_CONTROL_DEFAULT).post(body).build();
        //System.out.println(toString(request));
        Response response=client.newCall(request).execute();
        answer=response.body().string();
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

    public static Headers extendHeaders(String domain,String url,Map<String,String> headers){
        Headers.Builder builder=getHeadersBuilder(domain,url);
        if(headers!=null){headers.forEach(builder::set);}
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
        return load(url,domain,file,task,listener,false);
    }
    public static boolean load(String url, String domain, File file, Boolean cancel_flag, Callback2<Long,Long> listener, boolean skip){
        return load(client,url,domain,file,cancel_flag,listener,skip)==null;
    }
    public static Throwable load(OkHttpClient client,String url, String domain, File file, Boolean cancel_flag, Callback2<Long,Long> listener, boolean skip){
        return load(client,url,getHeadersDefault(domain,url),file,cancel_flag,listener,skip);
    }
    public static Throwable load(OkHttpClient client,String url, Headers headers, File file, Boolean cancel_flag, Callback2<Long,Long> listener, boolean skip){
        InputStream in=null;
        OutputStream out=null;
        try{
            Response response=client.newCall(new Request.Builder().url(url).headers(headers).get().build()).execute();
            if(response.isSuccessful()){
                long length=response.body().contentLength();
                long downloaded=file.getParentFile().mkdirs() ? 0 : file.length();
                if(downloaded==length){return null;}
                out=new FileOutputStream(file,skip);
                in=response.body().byteStream();
                byte[] arr=new byte[0x400]; long i=skip?in.skip(downloaded):0; int read;
                if(listener==null){
                    while((cancel_flag==null || !cancel_flag) && (read=in.read(arr))>0){
                        out.write(arr,0,read);
                    }
                }else{
                    while((cancel_flag==null || !cancel_flag) && (read=in.read(arr))>0){
                        out.write(arr,0,read); listener.call(i+=read,length);
                    }
                }
                out.close();
                in.close();
                if(cancel_flag!=null && cancel_flag){throw new IOException("Canceled");}
                if(file.length()<=downloaded){throw new IOException("Length of data is zero");}
                return null;
            }else{
                throw new HttpStatusException(response.code(),response.request().url().toString());
            }
        }catch(Exception e){
            e.printStackTrace();
            try{if(in!=null){in.close();}}catch(IOException e2){e2.printStackTrace();}
            try{if(out!=null){out.close();}}catch(IOException e2){e2.printStackTrace();}
            file.delete();
            return e;
        }
    }
}
