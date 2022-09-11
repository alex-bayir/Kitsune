package com.alex.edittextcode;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditTextCode extends androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView {
    public static class SyntaxHighlightRule{
        Pattern pattern; int color;
        public SyntaxHighlightRule(String pattern,int color){this.pattern=Pattern.compile(pattern); this.color=color;}
        public Pattern getPattern(){return pattern;}
        public int getColor(){return color;}
    }
    private SyntaxHighlightRule[] rules;
    private Set<String> suggestions;
    private final TextWatcher textWatcher=new TextWatcher() {
        int count=0; int start=-1;
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after){}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count){this.count=count; this.start=start;}
        @Override public void afterTextChanged(Editable s){
            if(count==1){
                switch (s.subSequence(start,start+count).toString()){
                    case " ":
                    case "\n":
                    case "(":
                    case ")": updateSyntax(); break;
                    default: break;
                }
            }
        }
    };
    private final Paint gutterPaint=new Paint();
    public EditTextCode(@NonNull Context context){this(context,null);}

    public EditTextCode(@NonNull Context context, @Nullable AttributeSet attrs){this(context, attrs,0);}

    public EditTextCode(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setHorizontallyScrolling(true);
        gutterPaint.setColor(getCurrentTextColor());
        setFocusable(true);
        setFocusableInTouchMode(true);
        setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        addTextChangedListener(textWatcher);
        setDropDownBackgroundResource(R.drawable.bg_suggestions);
    }

    @Override
    public boolean isSuggestionsEnabled(){return false;}

    private Spannable HighlightAllCode(Spannable s){
        if(rules!=null){
            for(SyntaxHighlightRule rule:rules){
                Matcher matcher=rule.getPattern().matcher(s.toString()).region(0,s.length());
                while(matcher.find()){
                    int start=matcher.start(),end=matcher.end();
                    if(start>=0 && end<=s.length() && start<end){
                        s.setSpan(new ForegroundColorSpan(rule.getColor()),start,end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }
        return s;
    }

    public void setSuggestionsSet(Set<String> suggestions){
        this.suggestions=suggestions;
        setTokenizer(new Tokenizer() {
            public static final String TOKEN = "!@#$%^&*()_+-={}|[]:;'<>/<.? \r\n\t";
            @Override
            public int findTokenStart(CharSequence text, int cursor) {
                int i=cursor;
                while (i>0 && !TOKEN.contains(text.subSequence(i-1,i))){i--;}
                while (i<cursor && text.charAt(i)==' '){i++;}
                return i;
            }

            @Override
            public int findTokenEnd(CharSequence text, int cursor) {
                int i=cursor;
                while (i<text.length()){
                    if(TOKEN.contains(text.subSequence(i-1,i))){return i;}else{i++;}
                }
                return text.length();
            }

            @Override
            public CharSequence terminateToken(CharSequence text){return text;}
        });
        setThreshold(1);
        setAdapter(new ArrayAdapter<String>(getContext(), R.layout.suggestion_item,new ArrayList<>()){
            @NonNull
            @Override
            public Filter getFilter() {
                return new Filter() {
                    @Override protected FilterResults performFiltering(CharSequence constraint){return null;}
                    @Override protected void publishResults(CharSequence constraint, FilterResults results){notifyDataSetChanged();}
                };
            }
        });
    }


    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if(getLayout()!=null){
            float x=getLayout().getPrimaryHorizontal(getSelectionStart());
            float y=getLayout().getLineBottom(getLayout().getLineForOffset(getSelectionStart()))+getTextSize();
            setDropDownHorizontalOffset((int)x+getPaddingLeft());
            setDropDownVerticalOffset((int)y-getScrollY());
        }
    }



    @Override
    protected Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results=new FilterResults();
                String input=constraint.toString();
                ArrayList<CharSequence> rs=new ArrayList<>();
                for(String suggestion:suggestions){
                    int start=suggestion.indexOf(input);
                    if(start>=0){
                        SpannableString str=new SpannableString(suggestion);
                        str.setSpan(new ForegroundColorSpan(0xFFFF0000),start,Math.max(start+input.length(),1),Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                        rs.add(str);
                    }
                }
                results.values=rs;
                results.count=rs.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                ArrayAdapter<CharSequence> adapter=((ArrayAdapter<CharSequence>)getAdapter());
                adapter.clear();
                adapter.addAll((ArrayList<CharSequence>)results.values);
                adapter.getFilter().filter(constraint);
                setDropDownWidth((int)getPaint().measureText(getWidestString((ArrayList<CharSequence>)results.values).toString())+50);
                if(results.count>0){
                    adapter.notifyDataSetChanged();
                }else{
                    adapter.notifyDataSetInvalidated();
                }
            }
        };
    }

    public CharSequence getWidestString(ArrayList<CharSequence> list){
        if(list==null || list.size()==0){return "";} int max=0; int len; CharSequence string=null;
        for(CharSequence str:list){if((len=str.length())>max){max=len; string=str;}}
        return string;
    }
    public void setText(String string){
        removeTextChangedListener(textWatcher);
        super.setText(HighlightAllCode(new SpannableString(string)), BufferType.SPANNABLE);
        addTextChangedListener(textWatcher);
    }
    public void updateSyntax(){
        int tmp=getSelectionStart();
        setText(getText().toString());
        setSelection(tmp);
    }
    public void setSyntaxHighlightRules(SyntaxHighlightRule[] rules){
        this.rules=rules;
        setText(getText().toString());
    }

    private static int countDigits(int number){int i=0; while (number>0){number/=10;i++;} return i;}

    @Override
    protected void onDraw(Canvas canvas) {
        int gutterPadding=16;
        gutterPaint.setTextSize(getPaint().getTextSize());
        float digitWidth=getPaint().measureText("0123456789")/10f;
        int width=(int)(countDigits(getLineCount())*digitWidth)+gutterPadding*3;
        if(getPaddingLeft()!=width){setPadding(width, getPaddingTop(), getPaddingRight(), getPaddingBottom());}
        super.onDraw(canvas);
        int lineHeight=getLineHeight();
        int line=getScrollY()/getLineHeight();
        int bottomVisibleLine=Math.min(line+getHeight()/lineHeight+2,getLineCount());
        float tmp=getPaddingLeft()+getScrollX()-gutterPadding;
        while (line++<bottomVisibleLine){
            canvas.drawText(
                    Integer.toString(line),
                    tmp-countDigits(line)*digitWidth-gutterPadding,
                    getLayout().getLineBaseline(line-1)+getPaddingTop(),
                    gutterPaint
            );
        }
        canvas.drawLine(tmp,getScrollY(),tmp,getScrollY()+getHeight(), gutterPaint);
    }
}
