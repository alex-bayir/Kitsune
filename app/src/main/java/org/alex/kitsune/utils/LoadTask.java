package org.alex.kitsune.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.TextView;
import androidx.annotation.NonNull;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.commons.HttpStatusException;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.manga.Manga_Scripted;
import org.alex.kitsune.services.LoadService;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public abstract class LoadTask<Params, Progress, Result>{

    private static final int publicProgress=0;
    private static final int onFinished=1;
    private static final int onBraked=2;
    private Handler handler;

    public static Message message(Object obj){return message(0,obj,0,0);}
    public static Message message(int what, Object obj){return message(what,obj,0,0);}
    public static Message message(int what, Object obj, int arg1, int arg2){
        Message msg=new Message();
        msg.what=what;
        msg.obj=obj;
        msg.arg1=arg1;
        msg.arg2=arg2;
        return msg;
    }
    public Handler getHandler(){return this.handler;}
    public final void start(Params params){
        handler=new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg){
                switch (msg.what){
                    case publicProgress: onProgressUpdate((Progress)msg.obj); break;
                    case onFinished: onFinished((Result)msg.obj); break;
                    case onBraked: onBraked((Throwable)msg.obj); break;
                    default: break;
                }
            }
        };
        new Thread(() -> {
            try{
                handler.sendMessage(message(onFinished, doInBackground(params)));
            }catch(Throwable th){
                handler.sendMessage(message(onBraked,th));
            }
        }).start();
    }

    protected abstract Result doInBackground(Params params);

    protected void onProgressUpdate(Progress progress){}

    public static void publicProgress(Handler handler, Object progress){if(handler!=null){handler.sendMessage(message(publicProgress,progress));}}
    public final void publicProgress(Object progress){publicProgress(handler,progress);}

    protected void onFinished(Result result){}

    public static void onBreak(Handler handler, Throwable throwable){if(handler!=null){handler.sendMessage(message(onBraked,throwable));}}
    public final void onBreak(Throwable throwable){onBreak(handler,throwable);}

    protected void onBraked(Throwable throwable){if(throwable!=null){throwable.printStackTrace();}}

    public static boolean loadInBackground(String url, String domain, File file, LoadService.Task task, Handler notify, boolean withSkip){
        InputStream in=null;
        FileOutputStream out=null;
        try{
            okhttp3.Response response=NetworkUtils.sHttpClient
                    .newCall(new okhttp3.Request.Builder()
                                    .url(url)
                                    .header(NetworkUtils.HEADER_USER_AGENT,NetworkUtils.USER_AGENT_DEFAULT)
                                    .header(NetworkUtils.HEADER_REFERER,domain!=null ? domain : url.substring(0,url.indexOf('/',8)))
                                    .get()
                                    .build()
                    ).execute();
            if(response.isSuccessful()){
                int responseLength=NetworkUtils.getContentLength(response);
                long downloadedSize=(!file.getParentFile().mkdirs() && !file.createNewFile()) ? file.length() : 0;
                if(downloadedSize==responseLength){return true;}
                out=new FileOutputStream(file,withSkip);
                in=Objects.requireNonNull(response.body()).byteStream();
                byte[] arr=new byte[0x400]; long i=0; int read;
                if(withSkip){i=in.skip(downloadedSize);}
                while((read=in.read(arr))>0 && (task==null || !task.isCanceled())){
                    out.write(arr,0,read);
                    if(notify!=null){i+=read; publicProgress(notify,responseLength>0 ? (Math.round(i/(float)responseLength*100)+"%") : (i/1024+"KB"));}
                }
                out.close();
                in.close();
                if(task!=null && task.isCanceled()){return false;}
            }else{throw new HttpStatusException(response.code(),response.request().url().toString());}
        }catch(Exception e){
            e.printStackTrace();
            try{if(in!=null){in.close();} if(out!=null){out.close();}}catch(IOException e2){e2.printStackTrace();}
            file.delete();
            onBreak(notify,e);
            return false;
        }
    return true;}

    public static void searchManga(String source, String query, int order, Callback<Collection<Manga>> callback){searchManga(source,query,order,callback,null);}
    public static void searchManga(String source, String query, int order, Callback<Collection<Manga>> callback, TextView out_error){
        if(callback!=null){
            new LoadTask<Object[], Void, ArrayList<Manga>>() {
                @Override
                protected ArrayList<Manga> doInBackground(Object[] query){
                    try{return Manga_Scripted.query((String)query[0],(String)query[1],(int)query[2],(int)query[3]);}catch(Exception e){onBreak(e);return null;}
                }
                @Override
                protected void onFinished(ArrayList<Manga> mangas){callback.call(mangas);}

                @Override
                protected void onBraked(Throwable e) {
                    super.onBraked(e);
                    out_error_info(e,out_error);
                }
            }.start(new Object[]{source,query,0,order});
        }
    }

    public static void out_error_info(Throwable e, TextView out_error){
        if(out_error!=null){
            if(Logs.checkType(e, SocketTimeoutException.class)){
                out_error.setText(R.string.time_out);
            }else if(Logs.checkType(e, HttpStatusException.class)) {
                out_error.setText(((HttpStatusException) e).description_code());
            }else if(Logs.checkType(e, SSLException.class)){
                out_error.setText(e.getClass().getSimpleName());
            }else{
                out_error.setText(R.string.nothing_found);
            }
        }
    }
}
