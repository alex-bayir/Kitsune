package org.alex.kitsune.commons;

import android.text.style.ClickableSpan;
import android.view.View;
import androidx.annotation.NonNull;

public class ClickSpan extends ClickableSpan {
    public interface SpanClickListener{void onClick(View view, CharSequence text);}

    public final CharSequence url;
    public final SpanClickListener listener;
    public ClickSpan(CharSequence text, SpanClickListener listener){
        this.url=text; this.listener=listener;
    }
    @Override
    public void onClick(@NonNull View view) {
        if(listener!=null){listener.onClick(view, url);}
    }
}
