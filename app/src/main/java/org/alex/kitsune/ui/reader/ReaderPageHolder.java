package org.alex.kitsune.ui.reader;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.LocaleList;
import android.view.*;
import android.view.textclassifier.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;
import com.vlad1m1r.lemniscate.base.BaseCurveProgressView;
import org.alex.kitsune.R;
import org.alex.kitsune.book.Chapter;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.book.Page;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.commons.CustomSnackbar;
import org.alex.kitsune.ocr.OCRImageView;
import org.alex.kitsune.ocr.Translate;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Utils;
import java.io.File;
import java.util.Locale;
import java.util.Random;

public class ReaderPageHolder extends RecyclerView.ViewHolder {
    public enum ScaleType {
        FIT_X(),
        FIT_Y(),
        FIT_XY(),
        CENTER();

        ScaleType(){}

        public static ScaleType valueOf(int mode){
            return switch (mode) {
                default -> FIT_X;
                case 1 -> FIT_Y;
                case 2 -> FIT_XY;
                case 3 -> CENTER;
            };
        }
    }

    OCRImageView image;
    Book book;
    Chapter chapter;
    Page page;
    File file;
    TextView text,progress,text_faces, text_info;
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

    @SuppressLint("ClickableViewAccessibility")
    ReaderPageHolder(ViewGroup parent, boolean vertical, Book book, View.OnClickListener centerClick, View.OnClickListener leftClick, View.OnClickListener rightClick){
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_page, parent, false));
        image=itemView.findViewById(R.id.image);
        text=itemView.findViewById(R.id.text);
        image.setMaximumScale(10);
        image.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            proportion=(bottom-top)/(float)(right-left);
        });

        image.setOnViewTapListener((view, x, y) -> {
            int width=view.getWidth(),length=width/3;
            boolean internal=!ReaderActivity.getSharedPreferences().getBoolean(Constants.use_another_translator,true);
            if(!image.showDialogTranslateIfTextExists(x,y,internal)){
                if(x<length){
                    leftClick.onClick(view);
                }else{
                    if(x>width-length){
                        rightClick.onClick(view);
                    }else{
                        centerClick.onClick(view);
                    }
                }
            }
        });

        text.setOnTouchListener(new View.OnTouchListener() {
            final GestureDetector detector=new GestureDetector(text.getContext(),new GestureDetector.SimpleOnGestureListener(){
                @Override
                public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                    float x=e.getX(),y=e.getY(); View view=text;
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
                    return true;
                }
            });
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return detector.onTouchEvent(event);
            }
        });
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            CustomSnackbar snackbar=ReaderActivity.getSnackbar();
            text.setTextClassifier(new Classifier(
                    text->{
                        if(text!=null){
                            String target_lang=Translate.getTarget_lang(itemView.getContext());
                            new Translate(target_lang).addOnCompleteListener(task->{
                                if(task.isSuccess()){
                                    snackbar.setText(task.getTranslated()).show();
                                }else{
                                    snackbar.dismiss();
                                }
                            }).translate(text.toString());
                        }else{
                            snackbar.dismiss();
                        }
                    })
            );
        }
        View view=LayoutInflater.from(itemView.getContext()).inflate(R.layout.dialog_input_url, null);
        dialog=new AlertDialog.Builder(itemView.getContext()).setView(view).create();
        input=view.findViewById(R.id.input);
        view.findViewById(R.id.close).setOnClickListener(v->dialog.cancel());
        view.findViewById(R.id.reset).setOnClickListener(v->input.setText(page!=null ? page.getUrl() : null));
        view.findViewById(R.id.retry).setOnClickListener(v->{retry(); dialog.dismiss();});
        progressBar=itemView.findViewById(R.id.progressBar);
        colorProgress=progressBar.getColor();
        progress=itemView.findViewById(R.id.SHOW_PROGRESS);
        load_info=itemView.findViewById(R.id.load_info);
        text_faces=itemView.findViewById(R.id.text_faces);
        text_faces.setText(faces[Math.abs(random.nextInt(faces.length))]);
        text_info=itemView.findViewById(R.id.text_info);
        retry=itemView.findViewById(R.id.retry);
        retry.setOnClickListener(v -> retry());
        change_url=itemView.findViewById(R.id.change_url);
        change_url.setOnClickListener(v->dialog.show());
        retry_layout=itemView.findViewById(R.id.retry_layout);
        load_info.setVisibility(View.GONE);
        retry_layout.setVisibility(View.GONE);
        this.book=book;
        this.vertical=vertical;
        itemView.getLayoutParams().height=vertical ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT;
        image.getLayoutParams().height=vertical ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT;
    }

    private void retry(){
        text_faces.setText(faces[Math.abs(random.nextInt(faces.length))]);
        if(file.length()==0){file.delete();}
        draw(input.getText().toString());
    }

    public void onBind(int position,Chapter chapter,ScaleType scaleType,boolean showTranslate){
        if(this.position!=position || this.chapter!=chapter){
            this.position=position;
            this.chapter=chapter;
            this.showTranslated=showTranslate;
            if(this.chapter!=null){
                file= book.getPage(this.chapter,page=this.chapter.getPage(position));
                draw(page.getUrl(),scaleType,vertical,showTranslate);
            }else{
                image.setImageDrawable(null);}
        }else if(this.showTranslated!=showTranslate){
            this.showTranslated=showTranslate;
            if(showTranslate){
                image.show();
            }else{
                image.hide();
            }
        }
    }
    public void draw(String url){
        draw(url,null,vertical,showTranslated);
    }
    public void draw(String url,ScaleType scaleType,boolean vertical,boolean showTranslate){
        if(file.exists()){
            image.setAdjustViewBounds(true);
            Drawable drawable=loadDrawable(file);
            image.setImageDrawable(drawable,file);
            if(drawable instanceof Animatable animatable){
                animatable.start();
            }
            setScaleType(scaleType,vertical);
            if(showTranslate){
                image.show();
            }else{
                image.hide();
            }
            if(drawable==null){
                try{
                    text.setText(Utils.File.readFile(file));
                    text.setVisibility(View.VISIBLE);
                    image.setVisibility(View.GONE);
                }catch (Exception ignored){}
            }else{
                text.setVisibility(View.GONE);
                image.setVisibility(View.VISIBLE);
            }
        }else{
            image.setImageDrawable(null);
            load_info.setVisibility(View.VISIBLE);
            retry_layout.setVisibility(View.GONE);
            progress.setText(R.string.loading);
            new Thread(()->{
                book.loadPage(chapter,page, f->{
                    NetworkUtils.getMainHandler().post(()->{
                        load_info.setVisibility(View.GONE);
                        retry_layout.setVisibility(View.GONE);
                        book.setLastTimeSave();
                        itemView.getContext().sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash, book.hashCode()).putExtra(Constants.option,Constants.load));
                        draw(url,scaleType,vertical,showTranslate);
                    });
                },null,(read, length)->{
                    float p = read / (float)length;
                    NetworkUtils.getMainHandler().post(()->{
                        if(0<p && p<=1){
                            progressBar.setColor(getColor(p));
                            progressBar.setLineMaxLength(p);
                            progressBar.setLineMinLength(p);
                            progress.setText(String.format(Locale.getDefault(),"Loaded %.1f%%",p*100));
                        }else{
                            progressBar.setColor(colorProgress);
                            progressBar.setLineMaxLength(1);
                            progressBar.setLineMinLength(0.01f);
                            progress.setText(R.string.loading);
                        }
                    });
                },e-> NetworkUtils.getMainHandler().post(()->{
                    load_info.setVisibility(View.GONE);
                    text_info.setText(String.format(Locale.getDefault(),"%s",e!=null?Utils.getRootCause(e,2).getMessage():"Failed to load resource"));
                    input.setText(url);
                    retry_layout.setVisibility(View.VISIBLE);
                }));
            }).start();
        }
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
            if(bitmap!=drawable.getBitmap()){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    drawable.setBitmap(bitmap);
                }else{
                    drawable=new BitmapDrawable(null,bitmap);
                }
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
        Drawable drawable=image.getDrawable();
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
        image.setScaleType(scaleType);
        image.setAdjustViewBounds(adjustViewBounds);
    }

    private int getColor(float progress){
        int maxI=colors.length-1;
        float k=1f/(maxI); int pos=1;
        while (pos<maxI && progress>k*pos){pos++;}
        return ColorUtils.blendARGB(colors[pos-1],colors[pos],(progress-k*(pos-1))*maxI);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static class Classifier implements TextClassifier {
        Callback<CharSequence> callback;
        public Classifier(Callback<CharSequence> callback){
            this.callback=callback;
        }
        @RequiresApi(api = Build.VERSION_CODES.P)
        @NonNull
        @Override
        public TextClassification classifyText(@NonNull TextClassification.Request request) {
            callback.call(request.getText().subSequence(request.getStartIndex(),request.getEndIndex()));
            return TextClassifier.super.classifyText(request);
        }
        @NonNull
        @Override
        public TextClassification classifyText(@NonNull CharSequence text, int startIndex, int endIndex, @Nullable LocaleList defaultLocales) {
            callback.call(text.subSequence(startIndex,endIndex));
            return TextClassifier.super.classifyText(text, startIndex, endIndex, defaultLocales);
        }
        @RequiresApi(api = Build.VERSION_CODES.P)
        @Override
        public boolean isDestroyed() {
            return TextClassifier.super.isDestroyed();
        }
        @RequiresApi(api = Build.VERSION_CODES.P)
        @Override
        public void destroy() {
            TextClassifier.super.destroy();
            callback.call(null);
        }
    }
}
