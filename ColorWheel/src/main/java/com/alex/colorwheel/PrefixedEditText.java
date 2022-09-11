package com.alex.colorwheel;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatEditText;

public class PrefixedEditText extends AppCompatEditText {
    private String prefix;
    private String Hint;
    private int prefixColor;
    public int textsize=10;
    public Paint paintforprefix;
    public String bold="";
    int boldstyle=0;
    public String boldStyle="";
    private int prefixPaddingBottom=0;
    private int prefixPaddingTop=0;
    private int prefixPaddingRight=0;
    private int prefixPaddingLeft=0;

    private int parseint(String s){
        int result=0; char c;
        for(int i=0;i<s.length();i++){
            c=s.charAt(i);
            if('0'<=c && c<='9'){result=result*10+c-'0';}
        }
    return result;}

    public PrefixedEditText(Context context){
        super(context);
        init();
        setPrefix("");
    }

    public PrefixedEditText(Context context, AttributeSet attrs){
        super(context,attrs);
        init();
        initPrefix(attrs);
    }

    public PrefixedEditText(Context context,AttributeSet attrs,int defStyle){
        super(context,attrs,defStyle);
        init();
        initPrefix(attrs);
    }

    protected void initPrefix(AttributeSet attrs){
        for(int i=0;i<attrs.getAttributeCount();i++){
            if(attrs.getAttributeName(i).equals("prefix")){setPrefix(attrs.getAttributeValue(i));}
            if(attrs.getAttributeName(i).equals("textSize")){textsize=(int)(parseint(attrs.getAttributeValue(i))/3.6f);}
            if(attrs.getAttributeName(i).equals("fontFamily")){bold=attrs.getAttributeValue(i);}
            if(attrs.getAttributeName(i).equals("textStyle")){boldStyle=attrs.getAttributeValue(i);}
            if(attrs.getAttributeName(i).equals("hint")){Hint=attrs.getAttributeValue(i);}
        }
        if(prefix==null){prefix="";}
        boldstyle=Typeface.BOLD;
        if(boldStyle!=null){
            switch(parseint(boldStyle)){
                case Typeface.BOLD: boldstyle=Typeface.BOLD; break;
                case Typeface.ITALIC: boldstyle=Typeface.ITALIC; break;
                case Typeface.BOLD_ITALIC: boldstyle=Typeface.BOLD_ITALIC; break;
                default : boldstyle=Typeface.NORMAL; break;
            }
        }
    }

    public String gethint(){return (Hint!=null) ? Hint : "";}

    protected void init(){
        setPrefixPaddings(getPaddingLeft(),getPaddingTop(),getPaddingRight(),getPaddingBottom());
    }

    public void setPrefix(String prefix) {
        this.prefix=prefix;
        if(paintforprefix==null){paintforprefix=new Paint(Paint.ANTI_ALIAS_FLAG);}
        setPrefixColor(prefixColor);
    }

    public String getPrefix(){return this.prefix;}

    public void setPrefixColor(int color){
        prefixColor=color;
        setPaintforprefix(paintforprefix);
    }
    public int getPrefixColor(){return prefixColor;}

    public void setTextsize(int textsize){
        this.textsize=textsize;
        setPaintforprefix(paintforprefix);
    }

    public void setPaintforprefix(Paint paint){
        paintforprefix=paint;
        if(paintforprefix!=null){
        if(prefix==null){prefix="";}
        paintforprefix.setColor(prefixColor);
        paintforprefix.setTextSize(textsize);
        if(bold==null){bold="serif-monospace";}
        paintforprefix.setTypeface(Typeface.create(bold,boldstyle));
        calculatePrefix(paintforprefix);
        invalidate();}
    }

    public void setPrefixPaddings(int left,int right,int top,int bottom){
        this.prefixPaddingLeft=left;
        this.prefixPaddingRight=right;
        this.prefixPaddingTop=top;
        this.prefixPaddingBottom=bottom;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        calculatePrefix(getPaint());
    }

    private void calculatePrefix(Paint paint){
        if(paint!=null){
            float[] widths=new float[prefix.length()];
            paint.getTextWidths(prefix, widths);
            float textWidth=0;
            for(float w : widths){textWidth+=w;}
            setPadding((int)(textWidth)+prefixPaddingLeft, prefixPaddingTop, prefixPaddingRight, prefixPaddingBottom);
        }
    }

    @Override
    public void onDraw(android.graphics.Canvas canvas){
        super.onDraw(canvas);
        canvas.drawText(prefix,prefixPaddingLeft, getLineBounds(0, null),(paintforprefix==null) ? getPaint() : paintforprefix);
    }
}
