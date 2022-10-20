package org.alex.kitsune.ui.reader;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;
import com.github.chrisbanes.photoview.PhotoView;
import com.vlad1m1r.lemniscate.base.BaseCurveProgressView;
import org.alex.kitsune.R;
import org.alex.kitsune.manga.Chapter;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.manga.Page;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.utils.LoadTask;
import java.io.File;
import java.util.HashSet;
import java.util.Random;

public class ReaderPageHolder extends RecyclerView.ViewHolder {
    public enum ScaleType {
        FIT_X(0),
        FIT_Y(1),
        FIT_XY(2),
        CENTER(3);

        ScaleType(int mode){}

        public static ScaleType valueOf(int mode){
            switch (mode){
                default:
                case 0: return FIT_X;
                case 1: return FIT_Y;
                case 2: return FIT_XY;
                case 3: return CENTER;
            }
        }
    }

    PhotoView imageView;
    Manga manga;
    Chapter chapter;
    Page page;
    File file;
    TextView progress,text_faces, text_info;
    BaseCurveProgressView progressBar;
    Button retry, change_url;
    View load_info,retry_layout;
    private float proportion=2;
    public static String[] faces=new String[]{"(￣ヘ￣)", "ヾ(`ヘ´)ﾉﾞ", "Σ(ಠ_ಠ)", "ಥ_ಥ", "(˘･_･˘)", "(；￣Д￣)", "(･Д･。)", "o(╥﹏╥)", "(◞ ‸ ◟ㆀ)", "(ᗒᗣᗕ)՞", "(-ω-、)", "(⋟﹏⋞)", "(ノ﹏ヽ)", "(T⌓T)", "(◕︿◕✿)", "⊙︿⊙", "(ノ_<。)ヾ(´ ▽ ` )", "ヽ(￣ω￣(。。 )ゝ","(ﾉ_；)ヾ(´ ∀ ` )","\t(っ´ω`)ﾉ(╥ω╥)","ρ(- ω -、)ヾ(￣ω￣; )","ヽ(~_~(・_・ )ゝ","(ｏ・_・)ノ”(ノ_<、)", "(＃`Д´)","(`皿´＃)","( ` ω ´ )","ヽ( `д´*)ノ","٩(╬ʘ益ʘ╬)۶","\t(╬ Ò﹏Ó)","(ﾉಥ益ಥ)ﾉ","(凸ಠ益ಠ)凸","▓▒░(°◡°)░▒▓", "(ᓀ ᓀ)","(⊙_⊙)","(づ ◕‿◕ )づ","(*￣ii￣)","\t|ʘ‿ʘ)╯","(^=◕ᴥ◕=^)","ヾ(=`ω´=)ノ”","ʕಠᴥಠʔ","(∩ᄑ_ᄑ)⊃━☆ﾟ*･｡*･:≡( ε:)","(⌐■_■)","(◕‿◕✿)"};
    private static final Random random=new Random();
    private static final HashSet<File> locks=new HashSet<>();
    int position;
    public static void clearLocks(){locks.clear();}
    final int colorProgress;
    private static final int[] colors=new int[]{0xFFFF0000,0xFFFFFF00,0xFF00FF00};
    private final boolean vertical;
    final Dialog dialog; TextView input;

    ReaderPageHolder(ViewGroup parent, boolean vertical, Manga manga, View.OnClickListener centerClick,View.OnClickListener leftClick,View.OnClickListener rightClick){
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_page, parent, false));
        imageView=itemView.findViewById(R.id.image);
        imageView.setMaximumScale(10);
        imageView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            proportion=(bottom-top)/(float)(right-left);
        });

        imageView.setOnViewTapListener((view, x, y) -> {
            int width=view.getWidth(),length=width/3;
            if(x<length){
                leftClick.onClick(view);
            }else{
                if(x>width-length){
                    rightClick.onClick(view);
                }else{
                    centerClick.onClick(view);
                }
            }
        });
        View view=LayoutInflater.from(itemView.getContext()).inflate(R.layout.dialog_input_url, null);
        dialog=new AlertDialog.Builder(itemView.getContext()).setView(view).create();
        input=view.findViewById(R.id.input);
        view.findViewById(R.id.close).setOnClickListener(v->dialog.cancel());
        view.findViewById(R.id.reset).setOnClickListener(v->input.setText(page!=null ? page.getUrl() : null));
        view.findViewById(R.id.retry).setOnClickListener(v->{retry(true); dialog.dismiss();});
        progressBar=itemView.findViewById(R.id.progressBar);
        colorProgress=progressBar.getColor();
        progress=itemView.findViewById(R.id.SHOW_PROGRESS);
        load_info=itemView.findViewById(R.id.load_info);
        text_faces=itemView.findViewById(R.id.text_faces);
        text_faces.setText(faces[Math.abs(random.nextInt(faces.length))]);
        text_info=itemView.findViewById(R.id.text_info);
        text_info.setVisibility(View.GONE);
        retry=itemView.findViewById(R.id.retry);
        retry.setOnClickListener(v -> retry(true));
        change_url=itemView.findViewById(R.id.change_url);
        change_url.setOnClickListener(v->dialog.show());
        retry_layout=itemView.findViewById(R.id.retry_layout);
        load_info.setVisibility(View.GONE);
        retry_layout.setVisibility(View.GONE);
        this.manga=manga;
        this.vertical=vertical;
        itemView.getLayoutParams().height=vertical ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT;
        imageView.getLayoutParams().height=vertical ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT;
    }

    private boolean retry(boolean trying){
        text_faces.setText(faces[Math.abs(random.nextInt(faces.length))]);
        if(trying){draw(input.getText().toString());}
        return trying;
    }

    public void onBind(int position,Chapter chapter,ScaleType scaleType){
        if(this.position!=position || this.chapter!=chapter){
            this.position=position;
            if(this.chapter!=chapter){
                clearPagesFromMemory(this.chapter);
            }
            this.chapter=chapter;
            if(this.chapter!=null){
                file=manga.getPage(this.chapter,page=this.chapter.getPage(position));
                draw(page.getUrl());
            }else{imageView.setImageDrawable(null);}
        }
        setScaleType(scaleType,vertical);
    }

    private void load(String url){
        load_info.setVisibility(View.VISIBLE);
        retry_layout.setVisibility(View.GONE);
        input.setText(url);
        progress.setText(R.string.loading);
        locks.add(file);
        new LoadTask<String,String,Boolean>(){
            @Override
            protected Boolean doInBackground(String url){
                return loadInBackground(url,manga.getProvider(),file,null,getHandler(),false);
            }

            @Override
            protected void onProgressUpdate(String s) {
                if(s.endsWith("%")){
                    float p=Integer.parseInt(s.substring(0,s.length()-1))/100f;
                    if(p>0 && p<=1){
                        progressBar.setColor(getColor(p));
                        progressBar.setLineMaxLength(p);
                        progressBar.setLineMinLength(p);
                    }else{
                        progressBar.setColor(colorProgress);
                        progressBar.setLineMaxLength(1);
                        progressBar.setLineMinLength(0.01f);
                    }
                }
                progress.setText("Loaded "+s);
            }

            @Override
            protected void onFinished(Boolean b){
                locks.remove(file);
                load_info.setVisibility(View.GONE);
                if(b){
                    retry_layout.setVisibility(View.GONE);
                    manga.setLastTimeSave();
                    itemView.getContext().sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash,manga.hashCode()).putExtra(Constants.option,Constants.load));
                    draw(url);
                }else{
                    retry_layout.setVisibility(View.VISIBLE);
                }
            }
        }.start(url);
    }

    public void draw(String url){
        Bitmap bitmap=ReaderActivity.imageCache.getFromMemory(manga.getPagePath(chapter,page));
        if(bitmap==null && !locks.contains(file)){
            bitmap=loadBitmap(file);
            if(!ReaderActivity.imageCache.addToMemory(manga.getPagePath(chapter,page),bitmap)){
                load(url);
            }
        }
        imageView.setImageBitmap(bitmap);
        text_info.setVisibility(bitmap==null && file.exists() ? View.VISIBLE : View.GONE);
        if(bitmap==null && file.exists()){
            String log=imageView.getContext().getString(R.string.impossible_decode_image);
            log+="\nFile size: "+file.length()+" bytes\nurl: "+page.getUrl();
            text_info.setText(log);
        }
    }

    public void setScaleType(ScaleType scaleType,boolean vertical){
        Drawable drawable=imageView.getDrawable();
        if(drawable!=null){
            switch (scaleType!=null ? scaleType : ScaleType.FIT_X) {
                case CENTER -> setScaleType(false, ImageView.ScaleType.CENTER);
                case FIT_XY -> setScaleType(vertical, ImageView.ScaleType.CENTER_CROP);
                case FIT_X -> setScaleType(vertical, drawable.getMinimumHeight() / (float) drawable.getMinimumWidth() > proportion ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
                case FIT_Y -> setScaleType(vertical, drawable.getMinimumHeight() / (float) drawable.getMinimumWidth() < proportion ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
            }
        }
    }
    private void setScaleType(boolean adjustViewBounds,ImageView.ScaleType scaleType){
        imageView.setScaleType(scaleType);
        imageView.setAdjustViewBounds(adjustViewBounds);
    }

    private Bitmap loadBitmap(File file){
        Bitmap bitmap=file.exists() ? BitmapFactory.decodeFile(file.getAbsolutePath()) : null;
        if(bitmap==null || bitmap.getByteCount()>100*1024*1024){
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize=bitmap!=null? ((int)Math.sqrt(bitmap.getByteCount()/((double)(100*1024*1024))))+1 : 2;
            bitmap=file.exists() ? BitmapFactory.decodeFile(file.getAbsolutePath(),options) : null;
        }
    return bitmap;}

    private void clearPagesFromMemory(Chapter chapter){
        if(chapter!=null && chapter.getPages()!=null){
            for(Page page:chapter.getPages()){
                ReaderActivity.imageCache.remove(manga.getPagePath(chapter,page));
            }
        }
    }
    private int getColor(float progress){
        int maxI=colors.length-1;
        float k=1f/(maxI); int pos=1;
        while (pos<maxI && progress>k*pos){pos++;}
        return ColorUtils.blendARGB(colors[pos-1],colors[pos],(progress-k*(pos-1))*maxI);
    }
}
