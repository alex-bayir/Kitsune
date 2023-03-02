package org.alex.kitsune.ui.reader;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.vlad1m1r.lemniscate.base.BaseCurveProgressView;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.manga.Chapter;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.manga.Page;
import org.alex.kitsune.ocr.OCRImageView;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Utils;
import java.io.File;
import java.io.InputStream;
import java.util.Locale;
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

    OCRImageView imageView;
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
    int position;
    final int colorProgress;
    private static final int[] colors=new int[]{0xFFFF0000,0xFFFFFF00,0xFF00FF00};
    private final boolean vertical;
    final Dialog dialog; TextView input;
    private boolean showTranslated=false;

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
        if(trying){
            if(file.length()==0){file.delete();}
            draw(input.getText().toString());
        }
        return trying;
    }

    public void onBind(int position,Chapter chapter,ScaleType scaleType,boolean showTranslate){
        if(this.position!=position || this.chapter!=chapter){
            this.position=position;
            this.chapter=chapter;
            this.showTranslated=showTranslate;
            if(this.chapter!=null){
                file=manga.getPage(this.chapter,page=this.chapter.getPage(position));
                draw(page.getUrl(),scaleType,vertical,showTranslate);
            }else{imageView.setImageDrawable(null);}
        }else if(this.showTranslated!=showTranslate){
            this.showTranslated=showTranslate;
            if(showTranslate){
                imageView.show();
            }else{
                imageView.hide();
            }
        }
    }
    public void draw(String url){
        draw(url,null,vertical,showTranslated);
    }
    public void draw(String url,ScaleType scaleType,boolean vertical,boolean showTranslate){
        if(file.exists()){
            imageView.setAdjustViewBounds(true);
            Drawable drawable=loadDrawable(file);
            imageView.setImageDrawable(drawable,file);
            if(drawable instanceof Animatable animatable){
                animatable.start();
            }
            setScaleType(scaleType,vertical);
            if(showTranslate){
                imageView.show();
            }else{
                imageView.hide();
            }
        }else{ //only load
            imageView.setImageDrawable(null);
            Glide.get(imageView.getContext()).getRegistry().replace(GlideUrl.class, InputStream.class, NetworkUtils.createFactoryForListenProgress((bytesRead, contentLength, done) -> {
                float p = Math.max(Math.min(bytesRead / (float)contentLength,1),0);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if(0<p && p<=1){
                        progressBar.setColor(getColor(p));
                        progressBar.setLineMaxLength(p);
                        progressBar.setLineMinLength(p);
                    }else{
                        progressBar.setColor(colorProgress);
                        progressBar.setLineMaxLength(1);
                        progressBar.setLineMinLength(0.01f);
                    }
                    progress.setText(String.format(Locale.getDefault(),"Loaded %.1f%%",p*100));
                });
            }));
            load_info.setVisibility(View.VISIBLE);
            retry_layout.setVisibility(View.GONE);
            progress.setText(R.string.loading);
            Glide.with(imageView.getContext()).download(NetworkUtils.getGlideUrl(url,manga.getProvider())).timeout(60000).listener(new RequestListener<>() {
                @Override
                public boolean onLoadFailed(@Nullable @org.jetbrains.annotations.Nullable GlideException e, Object model, Target<File> target, boolean isFirstResource) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        load_info.setVisibility(View.GONE);
                        text_info.setText(String.format(Locale.getDefault(),"%s",e!=null?Utils.getRootCause(e,2).getMessage():"Failed to load resource"));
                        input.setText(url);
                        retry_layout.setVisibility(View.VISIBLE);
                    });
                    return false;
                }
                @Override
                public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource, boolean isFirstResource) {
                    Utils.File.copy(resource, file);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        load_info.setVisibility(View.GONE);
                        retry_layout.setVisibility(View.GONE);
                        manga.setLastTimeSave();
                        itemView.getContext().sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash,manga.hashCode()).putExtra(Constants.option,Constants.load));
                        draw(url,scaleType,vertical,showTranslate);
                    });
                    return true;
                }
            }).submit();
        }
    }

    public ImageView.ScaleType calculate(Drawable drawable,ScaleType scaleType,boolean vertical,ImageView.ScaleType def){
        return drawable!=null? switch (scaleType!=null ? scaleType : ScaleType.FIT_X) {
            case CENTER -> ImageView.ScaleType.CENTER;
            case FIT_XY -> ImageView.ScaleType.CENTER_CROP;
            case FIT_X -> drawable.getMinimumHeight() / (float) drawable.getMinimumWidth() > proportion ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER;
            case FIT_Y -> drawable.getMinimumHeight() / (float) drawable.getMinimumWidth() < proportion ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER;
        }:def;
    }

    public static void loadDrawable(File file, Callback<Drawable> onLoad){
        if(onLoad==null){return;}
        new Thread(()->{
            Drawable drawable=loadDrawable(file);
            new Handler(Looper.getMainLooper()).post(()->onLoad.call(drawable));
        }).start();
    }
    public static Drawable loadDrawable(File file){
        Drawable drawable=Drawable.createFromPath(file.getAbsolutePath());
        if(drawable instanceof BitmapDrawable bd){
            drawable=adapt_size(bd,file);
        }
        return drawable;
    }
    public static BitmapDrawable adapt_size(BitmapDrawable drawable,File file){
        if(drawable!=null){
            Bitmap bitmap=adapt_size(drawable.getBitmap(),file);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                drawable.setBitmap(bitmap);
            }else{
                drawable=new BitmapDrawable(null,bitmap);
            }
        }
        return drawable;
    }
    public static Bitmap adapt_size(Bitmap bitmap,File file){
        if(bitmap==null || bitmap.getByteCount()>100*1024*1024){
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize=bitmap!=null? ((int)Math.sqrt(bitmap.getByteCount()/((double)(100*1024*1024))))+1 : 2;
            bitmap=file.exists() ? BitmapFactory.decodeFile(file.getAbsolutePath(),options) : null;
        }
        return bitmap;
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

    private int getColor(float progress){
        int maxI=colors.length-1;
        float k=1f/(maxI); int pos=1;
        while (pos<maxI && progress>k*pos){pos++;}
        return ColorUtils.blendARGB(colors[pos-1],colors[pos],(progress-k*(pos-1))*maxI);
    }
}
