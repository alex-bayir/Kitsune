package org.alex.kitsune.ui.reader;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.ColorUtils;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import com.vlad1m1r.lemniscate.base.BaseCurveProgressView;
import org.alex.kitsune.R;
import org.alex.kitsune.book.Chapter;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.book.Page;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.commons.CustomSnackbar;
import org.alex.kitsune.commons.ZoomTextView;
import org.alex.kitsune.ocr.OCRImageView;
import org.alex.kitsune.ocr.Translate;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Utils;
import java.io.File;
import java.util.Locale;
import java.util.Random;

public class ReaderPageHolder extends RecyclerView.ViewHolder {
    public enum ReaderMode {
        HorizontalRight(R.id.left_to_right),
        HorizontalLeft(R.id.right_to_left),
        Vertical(R.id.vertical),
        VerticalWeb(R.id.vertical_web);

        final int id;
        ReaderMode(int id){
            this.id=id;
        }

        public static ReaderMode valueOf(int mode){
            return switch (mode) {
                default -> HorizontalRight;
                case 1 -> HorizontalLeft;
                case 2 -> Vertical;
                case 3 -> VerticalWeb;
            };
        }
        public static ReaderMode fromId(int id){
            for(ReaderMode m:values()){if(m.id==id){return m;}} return null;
        }
    }
    public enum ScaleMode {
        FIT_X(R.id.fit_to_width),
        FIT_Y(R.id.fit_to_height),
        FIT_XY(R.id.fit_screen),
        CENTER(R.id.source_size);
        final int id;
        ScaleMode(int id){
            this.id=id;
        }

        public static ScaleMode valueOf(int mode){
            return switch (mode) {
                default -> FIT_X;
                case 1 -> FIT_Y;
                case 2 -> FIT_XY;
                case 3 -> CENTER;
            };
        }
        public static ScaleMode fromId(int id){
            for(ScaleMode m:values()){if(m.id==id){return m;}} return null;
        }
    }
    public static class Modes{
        public final ReaderMode R;
        public final ScaleMode S;
        public Modes(int R, int S){
            this(ReaderMode.valueOf(R), ScaleMode.valueOf(S));
        }
        public Modes(ReaderMode R, ScaleMode S){
            this.R=R;
            this.S=S;
        }
        public static Modes valueOf(int value){
            return new Modes(value/10,value%10);
        }
        @Override
        public int hashCode() {
            return R.ordinal()*10+S.ordinal();
        }
    }

    OCRImageView image;
    Book book;
    Chapter chapter;
    Page page;
    File file;
    ZoomTextView text;
    TextView progress,text_faces, text_info;
    BaseCurveProgressView progressBar;
    Button retry, change_url;
    View load_info,retry_layout;
    private float proportion=2;
    public static String[] faces=new String[]{"(￣ヘ￣)", "ヾ(`ヘ´)ﾉﾞ", "Σ(ಠ_ಠ)", "ಥ_ಥ", "(˘･_･˘)", "(；￣Д￣)", "(･Д･。)", "o(╥﹏╥)", "(◞ ‸ ◟ㆀ)", "(ᗒᗣᗕ)՞", "(-ω-、)", "(⋟﹏⋞)", "(ノ﹏ヽ)", "(T⌓T)", "(◕︿◕✿)", "⊙︿⊙", "(ノ_<。)ヾ(´ ▽ ` )", "ヽ(￣ω￣(。。 )ゝ","(ﾉ_；)ヾ(´ ∀ ` )","\t(っ´ω`)ﾉ(╥ω╥)","ρ(- ω -、)ヾ(￣ω￣; )","ヽ(~_~(・_・ )ゝ","(ｏ・_・)ノ”(ノ_<、)", "(＃`Д´)","(`皿´＃)","( ` ω ´ )","ヽ( `д´*)ノ","٩(╬ʘ益ʘ╬)۶","\t(╬ Ò﹏Ó)","(ﾉಥ益ಥ)ﾉ","(凸ಠ益ಠ)凸","▓▒░(°◡°)░▒▓", "(ᓀ ᓀ)","(⊙_⊙)","(づ ◕‿◕ )づ","(*￣ii￣)","\t|ʘ‿ʘ)╯","(^=◕ᴥ◕=^)","ヾ(=`ω´=)ノ”","ʕಠᴥಠʔ","(∩ᄑ_ᄑ)⊃━☆ﾟ*･｡*･:≡( ε:)","(⌐■_■)","(◕‿◕✿)"};
    private static final Random random=new Random();
    int position;
    private static final int[] colors=new int[]{0xFFFF0000,0xFFFFFF00,0xFF00FF00};
    private final Modes mode;
    private final Dialog dialog; TextView input;
    private boolean showTranslated=false;
    private final SharedPreferences prefs;

    @SuppressLint("ClickableViewAccessibility")
    ReaderPageHolder(ViewGroup parent, Modes mode, Book book, View.OnClickListener centerClick, View.OnClickListener leftClick, View.OnClickListener rightClick){
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_page, parent, false));
        prefs=PreferenceManager.getDefaultSharedPreferences(itemView.getContext());
        image=itemView.findViewById(R.id.image);
        text=itemView.findViewById(R.id.text);
        image.setMaximumScale(10);
        image.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            proportion=(bottom-top)/(float)(right-left);
        });

        image.setOnViewTapListener((view, x, y) -> {
            int width=view.getWidth(),length=width/3;
            boolean internal=!prefs.getBoolean(Constants.use_another_translator,true);
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
        text.setGestureListener(new GestureDetector.SimpleOnGestureListener(){
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
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            CustomSnackbar snackbar=((ReaderActivity)text.getContext()).getSnackbar();
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
            text.setOnFocusChangeListener((view, b) -> {
                if(!b){snackbar.dismiss();}
            });
        }
        View view=LayoutInflater.from(itemView.getContext()).inflate(R.layout.dialog_input_url, null);
        dialog=new AlertDialog.Builder(itemView.getContext()).setView(view).create();
        input=view.findViewById(R.id.input);
        view.findViewById(R.id.close).setOnClickListener(v->dialog.cancel());
        view.findViewById(R.id.reset).setOnClickListener(v->input.setText(page!=null ? page.getData() : null));
        view.findViewById(R.id.retry).setOnClickListener(v->{retry(); dialog.dismiss();});
        progressBar=itemView.findViewById(R.id.progressBar);
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
        this.mode=mode;
        itemView.getLayoutParams().height=(mode.R==ReaderMode.VerticalWeb ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void retry(){
        text_faces.setText(faces[Math.abs(random.nextInt(faces.length))]);
        if(file.length()==0){file.delete();}
        draw(input.getText().toString(),true,showTranslated);
    }

    public void onBind(int position,Chapter chapter,boolean translation){
        if(this.position!=position || this.chapter!=chapter){
            this.position=position;
            this.chapter=chapter;
            this.showTranslated=translation;
            if(this.chapter!=null){
                file=book.getPage(this.chapter,page=this.chapter.getPage(position));
                draw(page.getData(),translation);
            }else{
                image.setImageDrawable(null);}
        }else if(this.showTranslated!=translation){
            this.showTranslated=translation;
            if(translation){
                image.show();
            }else{
                image.hide();
            }
        }
    }
    public void draw(String url,boolean showTranslated){
        draw(url,Utils.isUrl(url),showTranslated);
    }
    public void draw(String url,boolean isUrl,boolean showTranslate){
        if(file.exists()){
            Drawable drawable=loadDrawable(file);
            image.setImageDrawable(drawable,file);
            if(drawable instanceof Animatable animatable){
                animatable.start();
            }
            setScaleType(mode.S, mode.R==ReaderMode.VerticalWeb);
            if(showTranslate){
                image.show();
            }else{
                image.hide();
            }
            if(drawable==null){
                try{
                    text.setText(Utils.File.read(file));
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
                Throwable error=book.load(chapter,page,url,isUrl,null,(read, length)->{
                    float p = read / (float)length;
                    NetworkUtils.getMainHandler().post(()->{
                        if(0<p && p<=1){
                            progressBar.setColor(getColor(p));
                            progressBar.setLineMaxLength(p);
                            progressBar.setLineMinLength(p);
                            progress.setText(String.format(Locale.getDefault(),"Loaded %.1f%%",p*100));
                        }else{
                            progressBar.setColor(0xffff0000);
                            progressBar.setLineMaxLength(1);
                            progressBar.setLineMinLength(0.01f);
                            progress.setText(R.string.loading);
                        }
                    });
                });
                if(error==null){
                    NetworkUtils.getMainHandler().post(()->{
                        load_info.setVisibility(View.GONE);
                        retry_layout.setVisibility(View.GONE);
                        book.setLastTimeSave();
                        itemView.getContext().sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash, book.hashCode()).putExtra(Constants.option,Constants.load));
                        draw(url,isUrl,showTranslate);
                    });
                }else{
                    NetworkUtils.getMainHandler().post(()->{
                        load_info.setVisibility(View.GONE);
                        text_info.setText(String.format(Locale.getDefault(),"%s",Utils.getRootCause(error,2).getMessage()));
                        input.setText(url);
                        retry_layout.setVisibility(View.VISIBLE);
                    });
                }
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

    public void setScaleType(ScaleMode scaleMode, boolean wrap_content){
        Drawable drawable=image.getDrawable();
        if(drawable!=null){
            switch (scaleMode !=null ? scaleMode : ScaleMode.FIT_X) {
                case CENTER -> setScaleType(wrap_content, ScaleType.CENTER);
                case FIT_XY -> setScaleType(wrap_content, ScaleType.CENTER_CROP);
                case FIT_X -> setScaleType(wrap_content, drawable.getMinimumHeight() / (float) drawable.getMinimumWidth() > proportion ? ScaleType.CENTER_CROP : ScaleType.FIT_CENTER);
                case FIT_Y -> setScaleType(wrap_content, drawable.getMinimumHeight() / (float) drawable.getMinimumWidth() < proportion ? ScaleType.CENTER_CROP : ScaleType.FIT_CENTER);
            }
        }
    }
    private void setScaleType(boolean adjustViewBounds,ScaleType scaleType){
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
