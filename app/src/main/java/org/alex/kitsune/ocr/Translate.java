package org.alex.kitsune.ocr;

import android.os.Handler;
import android.os.Looper;
import okhttp3.Request;
import okhttp3.Response;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.commons.URLBuilder;
import org.alex.kitsune.utils.NetworkUtils;
import java.util.LinkedList;
import org.json.JSONArray;
import org.json.JSONException;
import java.io.IOException;


public class Translate {
    final String source_lang;
    final String target_lang;
    private LinkedList<Callback<Task>> success;
    private LinkedList<Callback<Task>> failed;
    private LinkedList<Callback<Task>> complete;
    private final Handler handler=new Handler(Looper.getMainLooper());
    public Translate(String target_lang){
        this(null,target_lang);
    }
    public Translate(String source_lang,String target_lang){
        this.source_lang=source_lang;
        this.target_lang=target_lang;
        if(target_lang==null || target_lang.isEmpty()){
            throw new IllegalArgumentException("Target language must be initialized");
        }
    }

    public Translate addOnSuccessListener(Callback<Task> onSuccess){
        if(success==null){success=new LinkedList<>();}
        success.add(onSuccess);
        return this;
    }
    public Translate addOnFailedListener(Callback<Task> onFailed){
        if(failed==null){failed=new LinkedList<>();}
        failed.add(onFailed);
        return this;
    }
    public Translate addOnCompleteListener(Callback<Task> onComplete){
        if(complete==null){complete=new LinkedList<>();}
        complete.add(onComplete);
        return this;
    }
    public void translate(String text){
        if(text!=null && text.length()>1){
            new Thread(()->{
                Task task=new Task(source_lang,target_lang,text);
                task.translate();
                if(task.isSuccess()){
                    handler.post(()->callback(success,task));
                }else{
                    handler.post(()->callback(failed,task));
                }
                handler.post(()->callback(complete,task));
            }).start();
        }
    }
    private void callback(LinkedList<Callback<Task>> list,Task task){
        if(list!=null){
            for(Callback<Task> callback:list){
                callback.call(task);
            }
        }
    }

    private static JSONArray translate(String source_lang,String target_lang,String text) throws IOException, JSONException {
        String url=new URLBuilder("https://translate.googleapis.com/translate_a/single").add("client","gtx").add("sl",source_lang).add("tl",target_lang).add("dt","t").add("q",text).toString();
        Response response=NetworkUtils.sHttpClient.newCall(new Request.Builder().url(url).headers(NetworkUtils.getHeadersDefault()).build()).execute();
        JSONArray answer=new JSONArray(response.body().string());
        response.close();
        return answer;
    }

    private static String getSourceLanguage(String text){
        int[] lang=new int[4]; // en, ko, ja, zh-CN
        text.chars().forEach(v -> {
            if(in('A','Z',v) || in('a','z',v)){
                lang[0]++;
            } else if (in(0xAC00,0xD7A3,v) || in(0x1100,0x11FF,v) || in(0x3130,0x318F,v) || in(0xA960,0xA97F,v) || in(0xD7B0,0xD7FF,v)) {
                lang[1]++;
            } else if (in(0x3000,0x303f,v) || in(0x3040,0x309f,v) || in(0x30a0,0x30ff,v) || in(0xff00,0xffef,v) || in(0x4e00,0x9faf,v)) {
                lang[2]++;
            } else if (in(0x04E00,0x09FFF,v) || in(0x03400,0x04DBF,v) || in(0x20000,0x2A6DF,v) || in(0x2A700,0x2B73F,v) || in(0x2B740,0x2B81F,v) || in(0x2B820,0x2CEAF,v) || in(0x2CEB0,0x2EBEF,v) || in(0x30000,0x3134F,v) || in(0x31350,0x323AF,v) || in(0x0F900,0x0FAFF,v) || in(0x2F800,0x2FA1F,v) || in(0x02F00,0x02FDF,v) || in(0x02E80,0x02EFF,v)) {
                lang[3]++;
            }
        });
        int max=0,l=0;
        for(int i=0;i<lang.length;i++){
            if(lang[i]>max){max=lang[l=i];}
        }
        return switch (l){
            case 1->"ko";
            case 2->"ja";
            case 3->"zh";
            default->"en";
        };
    }

    private static boolean in(int start,int end,int value){
        return start<=value && value<=end;
    }
    static class Task{
        final String source_lang;
        final String target_lang;
        final String text;
        String translated;
        Throwable error;
        public Task(String source_lang,String target_lang,String text){
            this.source_lang=source_lang!=null?source_lang:getSourceLanguage(text);
            this.target_lang=target_lang;
            this.text=text;
        }
        public String getText(){
            return text;
        }
        public String getTranslated(){
            return translated;
        }

        public String getSource_lang(){
            return source_lang;
        }
        public String getTarget_lang(){
            return target_lang;
        }
        public Throwable getError(){
            return error;
        }
        public boolean isSuccess(){
            return error==null && text!=null;
        }
        public void translate(){
            try{
                JSONArray json=Translate.translate(source_lang,target_lang,text);
                json=json.getJSONArray(0);
                StringBuilder builder=new StringBuilder();
                for(int i=0;i<json.length();i++){
                    builder.append('\n').append(json.getJSONArray(i).optString(0));
                }
                translated=builder.substring(1);
            } catch (IOException|JSONException e) {
                error=e;
            }
        }
    }
}
