package org.alex.kitsune.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;
import info.guardianproject.netcipher.NetCipher;
import okhttp3.*;
import org.alex.kitsune.commons.SSLSocketFactoryExtended;
import org.alex.kitsune.services.MangaService;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class NetworkUtils {

    public static final String HEADER_REFERER = "Referer";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HTTP_DELETE = "DELETE";
    public static final String HTTP_GET = "GET";
    public static final String HTTP_POST = "POST";
    public static final String HTTP_PUT = "PUT";
    public static final String TAG = "NetworkUtils";
    public static final String TAG_ERROR = "NetworkUtils-error";
    public static final String TAG_REQUEST = "NetworkUtils-request";
    public static final String TAG_RESPONSE = "NetworkUtils-response";
    public static final String USER_AGENT_DEFAULT = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:65.0) Gecko/20100101 Firefox/65.0";
    public static final Headers HEADERS_DEFAULT = getHeadersDefault();
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
            List<Cookie> list=cookies.get(httpUrl.host());
            return list!=null ? list : new LinkedList<>();
        }
    }).readTimeout(60,TimeUnit.SECONDS).build();
    private static void printCookies(List<Cookie> cookies){if(cookies!=null)for(Cookie cookie:cookies){android.util.Log.e("Cookie",cookie.toString());}}

    public static Headers getHeadersDefault(){return getHeadersDefault(null);}
    public static Headers getHeadersDefault(String url){
        String cookie=MangaService.getCookieByUrl(url,null);
        if(cookie!=null){
            return new Headers.Builder().add(HEADER_USER_AGENT, USER_AGENT_DEFAULT).add("Cookie", cookie).build();
        }else{
            return new Headers.Builder().add(HEADER_USER_AGENT, USER_AGENT_DEFAULT).build();
        }
    }

    public static String getString(String url) throws IOException {
        return getString(url, null);
    }

    public static String getString(String url, okhttp3.Headers headers) throws IOException {
        String answer;
        Response response=sHttpClient.newCall(new Request.Builder().url(url).headers(headers!=null ? headers : getHeadersDefault(url)).cacheControl(CACHE_CONTROL_DEFAULT).get().build()).execute();
        try{answer=response.body().string();}catch (NullPointerException e){throw new IOException("ResponseBody is null");}
        response.close();
    return answer;}
    public static JSONObject getJSONObject(String str) throws IOException, JSONException {
        return new JSONObject(getString(str));
    }

    public static Document getDocument(String str) throws IOException {
        return getDocument(str, HEADERS_DEFAULT);
    }

    public static Document getDocument(String str, Headers headers) throws IOException {
        return Jsoup.parse(getString(str, headers), str);
    }
    public static int getContentLength(Response response) {
        String header=response.header("content-length");
        if(header==null){return -1;}
        try{return Integer.parseInt(header);}catch(NumberFormatException e){e.printStackTrace(); return -1;}
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
    public static Document httpGet(String url, String cookie) throws IOException {
        return httpGet(new URL(url),cookie);
    }
    public static Document httpGet(URL url, String cookie) throws IOException {
        NetCipher.setProxy(url.getHost(),8080);
        HttpURLConnection connection=NetCipher.getHttpURLConnection(url);
        if(connection instanceof HttpsURLConnection){
            Log.e("type","HttpsURLConnection");
            //HttpsURLConnection r3=(HttpsURLConnection)connection;
            HttpsURLConnection.setDefaultSSLSocketFactory(new SSLSocketFactoryExtended());
            connection.setRequestProperty("charset","utf-8");
        }
        Log.e("type",connection.getClass().getCanonicalName());
        if(TextUtils.isEmpty(cookie)){
            connection.setRequestProperty("Cookie",cookie);
        }
        connection.setConnectTimeout(15000);
        connection.connect();
        Log.e("Response","code="+connection.getResponseCode()+" message="+connection.getResponseMessage());
        InputStream in=connection.getInputStream();
        Document doc=Jsoup.parse(in,connection.getContentEncoding(),url.toString());
        if(in!=null){in.close();}
        Log.d("doc",doc.toString());
        return null;
    }

}
