package org.alex.kitsune.ocr;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import org.alex.kitsune.R;

public class DialogTranslate extends AlertDialog {
    Spinner source_lang,destination_lang;
    TextView source_text,destination_text;
    View reverse;
    protected DialogTranslate(Context context) {
        super(context);
        init(context);
    }

    protected DialogTranslate(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        init(context);
    }

    protected DialogTranslate(Context context, int themeResId) {
        super(context, themeResId);
        init(context);
    }

    private void init(Context context){
        View view=LayoutInflater.from(context).inflate(R.layout.dialog_translate,null);
        source_lang=view.findViewById(R.id.source_lang);
        destination_lang=view.findViewById(R.id.destination_lang);
        reverse=view.findViewById(R.id.reverse);
        source_text=view.findViewById(R.id.source_text);
        destination_text=view.findViewById(R.id.destination_text);
        setView(view);
        reverse.setOnClickListener(v->{
            int tmp=source_lang.getSelectedItemPosition();
            source_lang.setSelection(destination_lang.getSelectedItemPosition());
            destination_lang.setSelection(tmp);
            source_text.setText(destination_text.getText());
            translate(source_text.getText().toString());
        });
        source_lang.setAdapter(new ArrayAdapter<>(context, R.layout.simple_spinner_item, Translate.default_langs));
        destination_lang.setAdapter(new ArrayAdapter<>(context, R.layout.simple_spinner_item, Translate.default_langs));
        source_lang.setSelection(1);
    }
    public DialogTranslate init(String source,String translation){
        source_text.setText(source);
        destination_text.setText(translation);
        source_text.addTextChangedListener(new TextWatcher() {
            long last_edit_time=0;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                last_edit_time=System.currentTimeMillis();
                String text=s.toString();
                new Handler(Looper.getMainLooper()).postDelayed(()->{
                    if(System.currentTimeMillis()-last_edit_time>1000){
                        translate(text);
                    }
                },2000);
            }
        });
        return this;
    }
    public void translate(String text){
        new Translate(source_lang.getSelectedItem().toString(),destination_lang.getSelectedItem().toString())
                .addOnCompleteListener(task->{
                    if(task.isSuccess()){
                        destination_text.setText(task.getTranslated());
                    }
                })
                .translate(text);
    }
}
