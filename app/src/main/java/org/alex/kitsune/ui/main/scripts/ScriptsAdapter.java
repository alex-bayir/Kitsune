package org.alex.kitsune.ui.main.scripts;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.commons.HolderMenuItemClickListener;
import org.alex.kitsune.commons.HolderListener;
import org.alex.kitsune.utils.Utils;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;


public class ScriptsAdapter extends RecyclerView.Adapter<ScriptHolder> {
    File dir;
    ArrayList<File> files=new ArrayList<>();
    HolderListener holderListener;
    HolderMenuItemClickListener menuHolderListener;
    View.OnFocusChangeListener l;
    public ScriptsAdapter(File dir, View.OnFocusChangeListener l, HolderListener holderListener, HolderMenuItemClickListener menuHolderListener){
        this.dir=dir;
        this.holderListener=holderListener;
        this.menuHolderListener=menuHolderListener;
        this.l=l;
        update();

    }
    public void update(){
        files.clear();
        File[] tmp=dir.listFiles();
        if(tmp!=null){files.addAll(Arrays.asList(tmp));}
        notifyDataSetChanged();
    }
    public void update(File last,File next){
        if(Utils.File.move(last,next)){
            int index=files.indexOf(last);
            files.set(index,next);
            notifyItemChanged(index);
        }
    }
    @NonNull
    @NotNull
    @Override
    public ScriptHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        return new ScriptHolder(parent,l,holderListener,menuHolderListener);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ScriptHolder holder, int position) {
        holder.bind(files.get(position));
    }

    @Override
    public int getItemCount(){return files.size();}

    public void delete(int position){
        files.remove(position).delete();
        notifyItemRemoved(position);
    }

    public ArrayList<File> getFiles(){return this.files;}
}
