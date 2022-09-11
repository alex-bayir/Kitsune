package org.alex.kitsune.commons;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.R;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.util.*;


public class FilePickerPreference extends Preference implements Preference.SummaryProvider<FilePickerPreference> {
    Adapter adapter;
    public FilePickerPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public FilePickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FilePickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FilePickerPreference(Context context) {
        super(context);
        init();
    }

    private void init(){
        setSummaryProvider(this);
    }

    @Override
    protected void onClick() {
        if(getContext() instanceof Activity){
            Activity activity=(Activity)getContext();
            View v=activity.getLayoutInflater().inflate(R.layout.dialog_file_picker,null);
            adapter=new Adapter(new File(getValue()),v.findViewById(R.id.title));
            RecyclerView rv=v.findViewById(R.id.rv_list);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            rv.setAdapter(adapter);
            if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
                new AlertDialog.Builder(getContext())
                        .setTitle(getTitle()).setView(v)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            getSharedPreferences().edit().putString(getKey(),adapter.getDir().getAbsolutePath()).apply();
                            notifyChanged();
                            callChangeListener(getValue());
                        })
                        .setNegativeButton(android.R.string.cancel,null)
                        .create().show();
            }else{
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2478276);
            }
        }
    }

    public String getValue(){
        return getSharedPreferences().getString(getKey(),null);
    }

    @Override
    public CharSequence provideSummary(FilePickerPreference preference) {
        return preference.getValue();
    }

    public static class Adapter extends RecyclerView.Adapter<Adapter.Holder>{
        interface OnItemClickListener{
            void onItemClick(int position);
        }
        File dir;
        File[] list;
        TextView textView;
        OnItemClickListener listener=new OnItemClickListener() {
            @Override public void onItemClick(int position) {
                setDir(list[position]);
            }
        };
        public Adapter(File dir,TextView textView) {
            this.textView=textView;
            setDir(dir);
        }
        public void setDir(File dir){
            while(!dir.exists()){dir=dir.getParentFile();}
            Arrays.sort(list=dirList(dir),File::compareTo);
            textView.setText(dir.getAbsolutePath());
            notifyDataSetChanged();
        }
        public File getDir(){return dir;}
        private File[] dirList(File dir){
            this.dir=dir;
            if(dir.isDirectory()){
                File[] list=dir.listFiles(File::isDirectory);
                if(list==null){list=new File[0];}
                File[] l=new File[list.length+1];
                l[0]=dir.getParentFile();
                System.arraycopy(list,0,l,1,list.length);
                return l;
            }else{
                return new File[0];
            }
        }


        @NonNull
        @NotNull
        @Override
        public Adapter.Holder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file,parent,false));
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull Adapter.Holder holder, int position) {
            holder.bind(position==0 ? "..." : list[position].getName());
        }

        @Override
        public int getItemCount() {
            return list.length;
        }

        public class Holder extends RecyclerView.ViewHolder{
            ImageView image;
            TextView text;
            public Holder(@NonNull @NotNull View itemView) {
                super(itemView);
                image=itemView.findViewById(R.id.image);
                text=itemView.findViewById(R.id.text);
                itemView.setOnClickListener(v -> listener.onItemClick(getAdapterPosition()));
            }
            public void bind(String str){
                text.setText(str);
            }
        }
    }
}
