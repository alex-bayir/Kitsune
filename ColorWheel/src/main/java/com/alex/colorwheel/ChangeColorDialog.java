package com.alex.colorwheel;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import java.util.Objects;
import static com.alex.colorwheel.StringUtils.*;


public class ChangeColorDialog extends Dialog {
    public interface onColorChosen {
        void onChooseColor(int color);
    }
    private ColorPalette cp;
    private final String prefix="0x";
    private PrefixedEditText prefixedEditText;
    private boolean b=false;
    private final onColorChosen onColorChosen;

    public ChangeColorDialog(int lastColor, Context context, onColorChosen onColorChosen){
        super(context);
        this.onColorChosen=onColorChosen;
        init(lastColor);
    }
    public ChangeColorDialog(int lastColor, Context context, boolean cancelable, DialogInterface.OnCancelListener listener, onColorChosen onColorChosen){
        super(context,cancelable,listener);
        this.onColorChosen=onColorChosen;
        init(lastColor);
    }

    public void init(int color){
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        View v=getLayoutInflater().inflate(R.layout.fragment_color_chooser,null,false);
        prefixedEditText=v.findViewById(R.id.editcolorhex);
        cp=v.findViewById(R.id.ColorPalette);
        cp.setColor(color);

        prefixedEditText.setPrefix(prefix);
        prefixedEditText.setPrefixColor(0xFF5A5A5A);
        prefixedEditText.setHint(hintspanned(prefixedEditText.gethint()));
        prefixedEditText.setText(hexspanned(toHexString(color,"")));
        //TextInputLayout textInputLayout=v.findViewById(R.id.textinputlayout);
        //textInputLayout.setBoxStrokeColor(getContext().getColor(R.color.highlighting));
        prefixedEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count){
                if(Objects.requireNonNull(prefixedEditText.getText()).toString().length()>0){if(!prefixedEditText.getPrefix().equals(prefix)){prefixedEditText.setPrefix(prefix);}}else{prefixedEditText.setPrefix("");}
            }

            @Override
            public void afterTextChanged(Editable s){
                if(prefixedEditText.hasFocus()){
                    int curpos=prefixedEditText.getSelectionStart();
                    String str=s.toString();
                    int color=HexStringToInt(str);
                    str=parsehexstr(str).toUpperCase();
                    if(!b){b=true; prefixedEditText.setText(hexspanned(str)); prefixedEditText.setSelection(Math.min(curpos, str.length()));}
                    else{b=false; cp.setColor(color);}
                }
            }
        });

        cp.setOnColorChangeListener(new ColorPalette.OnColorChangeListener() {
            @Override
            public void onDismiss(int color) {
                onColorChosen.onChooseColor(color);
                cancel();
            }
            @Override
            public void onColorChanged(int color){prefixedEditText.setText(hexspanned(toHexString(color,"")));}
            @Override
            public void onFocusSetOnPalette(){prefixedEditText.clearFocus();}
        });
        setContentView(v);
    }

    public static Spannable hexspanned(String str){
        Spannable hexspan=new SpannableString(str);
        int l=str.length();
        if(l>0){hexspan.setSpan(new ForegroundColorSpan(Color.GRAY), 0, Math.min(l, 2), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if(l>2){hexspan.setSpan(new ForegroundColorSpan(Color.RED), 2, Math.min(l, 4), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                if(l>4){hexspan.setSpan(new ForegroundColorSpan(Color.GREEN), 4, Math.min(l, 6), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if(l>6){hexspan.setSpan(new ForegroundColorSpan(Color.BLUE), 6, Math.min(l, 8), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);}}}}
        return hexspan;
    }

    public static Spannable hintspanned(String str){
        Spannable spans=new SpannableString(str);
        int l=str.length();
        if(l>1){spans.setSpan(new ForegroundColorSpan(Color.GRAY), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if(l>2){spans.setSpan(new ForegroundColorSpan(Color.RED), 2, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                if(l>3){spans.setSpan(new ForegroundColorSpan(Color.GREEN), 3, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if(l>4){spans.setSpan(new ForegroundColorSpan(Color.BLUE), 4, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);}}}}
        return spans;
    }
}
