package org.alex.kitsune.logs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.BuildConfig;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.HolderClickListener;
import org.alex.kitsune.commons.HttpStatusException;
import org.jetbrains.annotations.NotNull;
import javax.net.ssl.SSLException;
import java.io.*;
import java.net.SocketTimeoutException;
import org.alex.kitsune.utils.Utils;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Logs {
    public static String dir;
    public static String getDir(){return dir;}
    public static void init(Context context){
        dir=context.getExternalFilesDir("logs").getAbsolutePath();
    }
    public static long saveLog(Throwable throwable){return saveLog(throwable,true);}
    public static long saveLog(Throwable throwable, boolean checkNetworkErrors){
        return saveLog(System.currentTimeMillis(),throwable,checkNetworkErrors);
    }
    public static long saveLog(long time,Throwable throwable, boolean checkNetworkErrors){
        return saveLog(dir+File.separator+time,time,throwable,checkNetworkErrors);
    }

    public static long saveLog(String path,long time,Throwable throwable, boolean checkNetworkErrors){
        if(throwable==null){return 0;}
        if(checkNetworkErrors && (checkType(throwable,SocketTimeoutException.class) || checkType(throwable,SSLException.class)) || checkType(throwable,HttpStatusException.class)){
            return time;
        }
        try{
            PrintWriter w=new PrintWriter(path);
            throwable.printStackTrace(w);
            throwable.printStackTrace();
            w.close();
            return time;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
    public static long saveLog(long time,String stack_trace,boolean checkNetworkErrors){
        if(stack_trace==null){return 0;}
        Log log=new Log(stack_trace,time);
        if(checkNetworkErrors && ("SocketTimeoutException".equals(log.getType()) || "SSLException".equals(log.getType()) || "HttpStatusException".equals(log.getType()))){
            return time;
        }
        try{
            PrintWriter w=new PrintWriter(dir+File.separator+time);
            w.print(stack_trace);
            w.close();
            return time;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
    public static boolean checkType(Throwable e, Class<? extends Throwable> type_class){
        return type_class.isAssignableFrom((e.getCause()!=null ? e.getCause() : e).getClass());
    }

    public static String getStackTrace(Throwable throwable){
        if(throwable==null){return null;}
        StringWriter sw=new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
    public static void sendLog(Context context,long date){
        sendLog(context,new Log(new File(dir+"/"+date)));
    }
    public static void sendLog(Context context,Log log){
        sendLog(context,log.getType(),log.stackTrace);
    }
    public static void sendLog(Context context,String subject,String log){
        context.startActivity(Intent.createChooser(
                new Intent(Intent.ACTION_SENDTO,new Uri.Builder().scheme("mailto").build())
                        .putExtra(Intent.EXTRA_EMAIL,new String[]{"kitsune.logs@gmail.com"})
                        .putExtra(Intent.EXTRA_SUBJECT,subject)
                        .putExtra(Intent.EXTRA_TEXT,log)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ,context.getString(R.string.send_log)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    public static void e(Object log){e(log!=null?toString(log):"null");}
    public static void d(Object log){d(log!=null?toString(log):"null");}
    public static void e(String log){android.util.Log.e("Debug",log!=null?log:"null");}
    public static void d(String log){android.util.Log.d("Debug",log!=null?log:"null");}
    private static String toString(Object obj){
        return obj==null?"null":(obj instanceof Object[] objs?arr_to_string(objs):obj.toString());
    }
    public static String arr_to_string(Object[] objs){
        StringBuilder builder=new StringBuilder("[");
        boolean first=true;
        for(Object obj:objs){
            if(first){first=false;}else{builder.append(",");} builder.append(obj);
        }
        builder.append("]");
        return builder.toString();
    }

    public static boolean clearLog(long date){
        return new File(dir+"/"+date).delete();
    }
    public static void clearAll(){
        File[] list=new File(dir).listFiles(File::isFile);
        for(int i=0;i<(list!=null ? list.length:0);i++){
            list[i].delete();
        }
    }

    public static ArrayList<Log> getLogs(){
        File[] list=new File(dir).listFiles(pathname -> !pathname.isDirectory());
        ArrayList<Log> logs=new ArrayList<>(list!=null ? list.length : 0);
        if(list!=null){for(File file : list){logs.add(new Log(file));}}
        Collections.sort(logs, (o1, o2) -> Long.compare(o2.date,o1.date));
        return logs;
    }
    public static HashMap<String,ArrayList<Log>> getLogs(ArrayList<Log> logs){
        HashMap<String,ArrayList<Log>> map=new HashMap<>();
        for(Log log:logs){
            String type=log.getType();
            if(!map.containsKey(type)){map.put(type,new ArrayList<>());}
            map.get(type).add(log);
        }
        return map;
    }

    public static class Log{
        public final String stackTrace;
        public final long date;
        public final String type;
        private static String readFile(File f){
            byte[] bytes=new byte[(int)f.length()];
            try{
                FileInputStream in=new FileInputStream(f);
                in.read(bytes);
                in.close();
            }catch (IOException e) {
                e.printStackTrace();
            }
            return new String(bytes);
        }

        public Log(File file){
            this(file,Long.parseLong(file.getName()));
        }
        public Log(File file,long time){
            this(readFile(file),time);
        }
        public Log(String stackTrace,long date){
            this.stackTrace=stackTrace;
            this.date=date;
            this.type=getType(stackTrace);
        }
        public SpannableString getSpannedString(){
            return getSpannedString(stackTrace);
        }
        public static SpannableString getSpannedString(String stackTrace){
            SpannableString str=new SpannableString(stackTrace);
            Matcher matcher=Pattern.compile(BuildConfig.APPLICATION_ID+".*\\(([\\w:\\s.]+)\\)").matcher(str);
            while(matcher.find()){
                int start=matcher.start(1),end=matcher.end(1);
                System.out.println(str.subSequence(start,end));
                str.setSpan(new UnderlineSpan(),start,end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                str.setSpan(new ForegroundColorSpan(0xFF0022FF),start,end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return str;
        }
        public static String getType(String stackTrace){
            String line=Utils.group(stackTrace,"^[\\S]+\\.(\\w+):?[^\\n]+",null);
            return line.length()==0?"Unknown":line;
        }
        public String getType(){
            return type;
        }
        public static final SimpleDateFormat simpleDateFormat=new SimpleDateFormat("dd.MM.yyyy HH:mm:ss ",Locale.US);
        public static String date(long date){return Utils.date(date,simpleDateFormat);}
        public String date(){return date(date);}
    }
    public static class LogsAdapter extends RecyclerView.Adapter<LogHolder> {
        interface OnLogRemoveListener {
            void onItemRemove(Log log);
        }
        private final HolderClickListener listener;
        private final HolderClickListener clear=this::clearLog;
        private final OnLogRemoveListener onRemove;
        final List<Log> logs;

        public LogsAdapter(List<Log> logs, HolderClickListener listener, OnLogRemoveListener onRemove){
            this.logs=logs;
            this.listener=listener;
            this.onRemove=onRemove;
        }
        public void setLogs(List<Log> logs){
            this.logs.clear();
            this.logs.addAll(logs);
            notifyDataSetChanged();
        }
        public Log getLog(int pos){return logs.get(pos);}
        public void clearLog(View v,int position){
            Log log=logs.remove(position);
            Logs.clearLog(log.date);
            if(onRemove!=null){onRemove.onItemRemove(log);}
            notifyItemRemoved(position);
        }
        public void clearAll(){
            for(Log log:logs){Logs.clearLog(log.date);}
            int size=logs.size();
            logs.clear();
            notifyItemRangeRemoved(0,size);
        }
        @NonNull
        @NotNull
        @Override
        public LogHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            return new LogHolder(parent,listener,clear);
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull LogHolder holder, int position) {
            holder.bind(logs.get(position));
        }

        @Override
        public int getItemCount(){return logs.size();}
    }


    public static class LogHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView type,log_text, date_text;
        Button clear;
        Log log;
        public LogHolder(ViewGroup parent,HolderClickListener listener,HolderClickListener listener_button) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log,parent,false));
            card=(CardView) itemView;
            type=card.findViewById(R.id.type);
            log_text=card.findViewById(R.id.log);
            log_text.setHorizontallyScrolling(true);
            clear=card.findViewById(R.id.clear);
            date_text=card.findViewById(R.id.time);
            if(listener!=null){
                card.setOnClickListener(v -> listener.onItemClick(v,getBindingAdapterPosition()));
            }
            if(listener_button!=null){
                clear.setOnClickListener(v -> listener_button.onItemClick(v,getBindingAdapterPosition()));
            }
        }
        public void bind(Log log){
            this.log=log;
            type.setText(log.getType());
            log_text.setText(log.stackTrace);
            date_text.setText(log.date());
        }
    }
    public static AlertDialog createDialog(Context context, Throwable throwable){return createDialog(context, throwable,System.currentTimeMillis());}
    public static AlertDialog createDialog(Context context, Throwable throwable,long date){
        View root=LayoutInflater.from(context).inflate(R.layout.content_log,null);
        root.setBackgroundColor(0);
        TextView stack_trace=root.findViewById(R.id.log_text),log_date=root.findViewById(R.id.log_date);
        stack_trace.setHorizontallyScrolling(true);
        Writer writer=new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        stack_trace.setText("Type: "+(throwable.getCause()!=null ? throwable.getCause() : throwable).getClass().getName()+"\nCause: "+throwable.getMessage()+"\nStackTrace:\n"+writer);
        log_date.setText(Log.date(date));
        root.findViewById(R.id.send).setOnClickListener(v->sendLog(context,date));
        final AlertDialog dialog=new AlertDialog.Builder(context).setView(root).create();
        root.findViewById(R.id.cancel).setOnClickListener(v1 -> dialog.cancel());
        return dialog;
    }
}
