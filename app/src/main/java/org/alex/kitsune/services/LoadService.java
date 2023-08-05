package org.alex.kitsune.services;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.*;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.selection.Selection;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.book.Page;
import org.alex.kitsune.book.Chapter;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class LoadService extends Service {
    public static final String actionCancel = LoadService.class.getName() + ".action_cancel";
    public static final String actionLoad = LoadService.class.getName() + ".action_load";
    private final String text = "Text", name = "name", id = "id", cover = "cover", max = "max", progress = "Progress", indeterminate = "indeterminate", channelID = "download", close = "close";
    public final Context context = this;
    public static final int notificationId = 0;
    private NotificationManagerCompat notificationManagerCompat;
    private boolean executing = false;
    private NotificationCompat.InboxStyle style;
    ArrayList<Task> tasks = new ArrayList<>();
    Task currentTask;

    private void addTask(Task task) {
        if (task != null) {
            tasks.add(task);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManagerCompat = NotificationManagerCompat.from(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManagerCompat.createNotificationChannel(new NotificationChannel(channelID, channelID, NotificationManager.IMPORTANCE_DEFAULT));
        }
    }

    public LoadService() {
        super();
    }

    private List<Page> getPages(Book book, Chapter chapter){
        try{return book.getPages(chapter);}catch(Exception e){Logs.saveLog(e); return null;}
    }
    private static void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void clearTasks(){
        tasks.clear();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("action", intent.getAction());
        addTask(Task.fromIntent(intent));
        if (!executing && actionLoad.equals(intent.getAction())) {
            executing = true;
            style = new NotificationCompat.InboxStyle();
            new Thread(() -> {
                try {
                    int download = 0;
                    while (tasks.size() > 0) {
                        currentTask = tasks.remove(0);
                        Task task = currentTask;
                        int tmp = download;
                        for (int i : task.indexes) {
                            Chapter chapter = task.book.getChapters().get(i);
                            Bundle bundle = createBundle(task);
                            bundle.putString(text, "getting urls...");
                            bundle.putInt(max, 1);
                            publicProgress(bundle);
                            while (getPages(task.book,chapter) == null) {
                                if (task.isCanceled()) {
                                    task.clearCancel();
                                    finish(download); clearTasks(); return;
                                }
                                sleep(1000);
                            }
                            bundle = createBundle(task);
                            bundle.putString(text, chapter.text(context));
                            bundle.putInt(max, chapter.getPages().size());
                            bundle.putBoolean(indeterminate, false);
                            publicProgress(bundle);
                            int pages = 0;
                            for (Page page : chapter.getPages()) {
                                while (!task.book.load(chapter,page, f->{},task.isCanceled(),null,null)) {
                                    if (task.isCanceled()) {
                                        task.clearCancel();
                                        sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash, task.book.hashCode()).putExtra(Constants.option, Constants.load));
                                        finish(download); clearTasks(); return;
                                    }
                                    sleep(1000);
                                }
                                bundle = createBundle(task);
                                bundle.putString(text, chapter.text(context));
                                bundle.putInt(max, chapter.getPages().size());
                                bundle.putInt(progress, ++pages);
                                bundle.putBoolean(indeterminate, false);
                                publicProgress(bundle);
                            }
                            task.book.setLastTimeSave();
                            task.book.save();
                            BookService.allocate(task.book,false);
                            sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash, task.book.hashCode()).putExtra(Constants.option, Constants.load));
                            download++;
                        }
                        if (task.book != null) {
                            style.addLine(task.book.getName() + ": " + Math.max(Math.min(download - tmp, task.indexes.size()), 0) + " saved");
                            Bundle bundle = createBundle(task);
                            bundle.putBoolean(close, true);
                            publicProgress(bundle);
                        }
                    }
                    finish(download); return;
                } catch (Throwable e) {
                    onBreak(e);
                }
            }).start();
        }
        if (actionCancel.equals(intent.getAction())) {
            if (currentTask != null) {
                currentTask.cancel();
            }
            notificationManagerCompat.cancel(intent.getIntExtra(id, -1));
        }
        return START_NOT_STICKY;
    }

    private void publicProgress(Bundle b) {
        NetworkUtils.getMainHandler().post(() -> {
            if (b.getBoolean(close, false)) {
                notificationManagerCompat.cancel(b.getInt(id)); return;
            }
            Intent intent = new Intent(context, LoadService.class);
            intent.setAction(LoadService.actionCancel);
            intent.putExtra(id, b.getInt(id));
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            Notification notification = new NotificationCompat.Builder(context, b.getString(name))
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setLargeIcon(BitmapFactory.decodeFile(b.getString(cover)))
                    .setContentTitle(b.getString(name))
                    .setContentText(b.getString(text))
                    .setProgress(b.getInt(max, 0), b.getInt(progress, 0), b.getBoolean(indeterminate, true))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                    .setChannelId(channelID)
                    .setSilent(true)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", pendingIntent)
                    .build();
            notificationManagerCompat.notify(b.getInt(id), notification);
        });
    }

    private void onBreak(Throwable e){
        NetworkUtils.getMainHandler().post(e::printStackTrace);
    }

    private void finish(int download){
        NetworkUtils.getMainHandler().post(()->{
            notificationManagerCompat.cancelAll();
            executing=false;
            if(download>0){
                Notification notification=new NotificationCompat.Builder(context,channelID)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setContentTitle("Kitsune")
                        .setContentText(download+" chapters saved")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                        .setChannelId(channelID)
                        .setOngoing(false)
                        .setAutoCancel(true)
                        .setStyle(style)
                        .build();
                notificationManagerCompat.notify(notificationId,notification);
            }
        });
    }

    private Bundle createBundle(Task task){
        Bundle bundle=new Bundle();
        bundle.putString(name,task.book.getName());
        bundle.putInt(id,task.book.hashCode());
        bundle.putString(cover,task.book.getCoverPath());
    return bundle;}

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("service","onDestroy");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    public static class Task{
        final Book book;
        ArrayList<Integer> indexes;
        private Boolean canceled=false;
        public Task(Book book, int start, int end){
            this.book=book;
            indexes=IntStream.rangeClosed(start, end).boxed().collect(Collectors.toCollection(ArrayList::new));
        }
        public Task(Book book, Selection<Long> selection){
            this(book,Utils.convert(selection));
        }
        public Task(Book book, ArrayList<Integer> indexes){
            this.book=book;
            this.indexes=indexes;
        }

        public void clearCancel(){canceled=false;}
        public void cancel(){canceled=true;}
        public Boolean isCanceled(){return canceled;}

        public Intent toIntent(Intent intent){
            return intent!=null ? intent.putExtra(Constants.hash, book.hashCode()).putIntegerArrayListExtra("indexes",indexes) : null;
        }

        public static Task fromIntent(Intent intent){
            return new Task(BookService.get(intent.getIntExtra(Constants.hash,-1)),intent.getIntegerArrayListExtra("indexes"));
        }
    }
}
