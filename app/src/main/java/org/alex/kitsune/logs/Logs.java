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
import org.alex.kitsune.R;
import org.alex.kitsune.commons.HolderClickListener;
import org.jetbrains.annotations.NotNull;
import javax.net.ssl.SSLException;
import java.io.*;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class Logs {
    public static String dir;
    public static String getDir(){return dir;}
    public static void init(Context context){
        dir=context.getExternalFilesDir("logs").getAbsolutePath();
        Thread.setDefaultUncaughtExceptionHandler((paramThread, paramThrowable) -> {
            Logs.saveLog(paramThrowable);
            System.exit(2);
        });
    }
    public static long saveLog(Throwable throwable){return saveLog(throwable,true);}
    public static long saveLog(Throwable throwable, boolean checkNetworkErrors){
        if(throwable==null){return 0;}
        if(checkNetworkErrors && (checkType(throwable,SocketTimeoutException.class) || checkType(throwable,SSLException.class))){
            return System.currentTimeMillis();
        }
        try{
            long time=System.currentTimeMillis();
            PrintWriter w=new PrintWriter(dir+File.separator+time);
            throwable.printStackTrace(w);
            throwable.printStackTrace();
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
        Log log=new Log(new File(dir+"/"+date));
        context.startActivity(Intent.createChooser(
                new Intent(Intent.ACTION_SENDTO,new Uri.Builder().scheme("mailto").build())
                        .putExtra(Intent.EXTRA_EMAIL,new String[]{"alex8888499@gmail.com"})
                        .putExtra(Intent.EXTRA_SUBJECT,"Kitsune logs "+log.getType())
                        .putExtra(Intent.EXTRA_TEXT,log.stackTrace)
        ,context.getString(R.string.send_log)));
    }
    public static void e(Object log){e(log!=null?log.toString():"null");}
    public static void d(Object log){d(log!=null?log.toString():"null");}
    public static void e(String log){android.util.Log.e("Debug",log);}
    public static void d(String log){android.util.Log.d("Debug",log);}

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
        public final Throwable throwable;
        public final String stackTrace;
        public final long date;
        public Log(Throwable throwable){
            this.throwable=throwable;
            this.date=System.currentTimeMillis();
            stackTrace=null;
        }
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
            this(readFile(file),Long.parseLong(file.getName()));
        }
        public Log(String stackTrace,long date){
            throwable=null;
            this.stackTrace=stackTrace;
            this.date=date;
        }
        public SpannableString getSpannedString(){
            return getSpannedString(stackTrace);
        }
        public static SpannableString getSpannedString(String stackTrace){
            SpannableString str=new SpannableString(stackTrace);
            int start,end=0;
            String package_s=Logs.class.getPackage().getName();
            while ((start=stackTrace.indexOf(package_s,end))>=0){
                start=stackTrace.indexOf("(",start)+1;
                end=stackTrace.indexOf(")",start);
                str.setSpan(new UnderlineSpan(),start,end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                str.setSpan(new ForegroundColorSpan(0xFF0022FF),start,end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return str;
        }
        public String getType(){
            try{
                String tmp=stackTrace.substring(0,stackTrace.indexOf(":"));
                return tmp.substring(tmp.lastIndexOf(".")+1);
            }catch (Throwable throwable){
                return null;
            }
        }
        public static final SimpleDateFormat simpleDateFormat=new SimpleDateFormat("ss:mm:HH dd.MM.yyyy");
        public static String date(long date, DateFormat dateFormat){return dateFormat.format(new Date(date));}
        public static String date(long date){return date(date,simpleDateFormat);}
        public String date(){return date(date);}
    }
    public static class LogsAdapter extends RecyclerView.Adapter<LogHolder> {
        interface OnCountChangeListener{
            void onCountChange(int count);
        }
        interface OnLogRemoveListener {
            void onItemRemove(Log log);
        }
        private final HolderClickListener listener;
        private final HolderClickListener clear=this::clearLog;
        private final OnCountChangeListener onCountChangeListener;
        private final OnLogRemoveListener onRemove;
        final List<Log> logs;

        public LogsAdapter(List<Log> logs, HolderClickListener listener, OnCountChangeListener onCountChangeListener, OnLogRemoveListener onRemove){
            this.logs=logs;
            this.listener=listener;
            this.onCountChangeListener=onCountChangeListener;
            onCountChangeListener.onCountChange(getItemCount());
            this.onRemove=onRemove;
        }
        public void setLogs(List<Log> logs){
            this.logs.clear();
            this.logs.addAll(logs);
            onCountChangeListener.onCountChange(getItemCount());
            notifyDataSetChanged();
        }
        public Log getLog(int pos){return logs.get(pos);}
        public void clearLog(View v,int position){
            Log log=logs.remove(position);
            Logs.clearLog(log.date);
            if(onRemove!=null){onRemove.onItemRemove(log);}
            notifyItemRemoved(position);
            onCountChangeListener.onCountChange(getItemCount());
        }
        public void clearAll(){
            for(Log log:logs){Logs.clearLog(log.date);}
            int size=logs.size();
            logs.clear();
            notifyItemRangeRemoved(0,size);
            onCountChangeListener.onCountChange(getItemCount());
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
