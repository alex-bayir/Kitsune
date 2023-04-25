package org.alex.kitsune.book.search;

import android.text.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.ClickSpan;
import org.alex.kitsune.commons.DiffCallback;
import org.alex.kitsune.commons.HolderClickListener;
import com.alex.threestates.ThreeStatesTextView;
import org.alex.kitsune.scripts.Script;
import org.alex.kitsune.book.search.Options.StringPair;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class FilterSortAdapter extends RecyclerView.Adapter<FilterSortAdapter.PairHolder> {

    private final Script script;
    private final Options[] options;
    private final ArrayList<StringPair> pairs=new ArrayList<>(200);
    public FilterSortAdapter(Script script,List<Options> options){
        this(script,options!=null ? options.toArray(new Options[0]) : new Options[0]);
    }
    public FilterSortAdapter(Script script,Options... options){
        this.options=options;
        this.script=script;
        update_pairs();
        reset(false);
    }

    public void update_pairs(){
        new DiffCallback<>(pairs,()->{
            pairs.clear();
            for(Options option: options){
                if(option!=null){
                    pairs.add(option.title);
                    if(option.title.getChecked()==0){
                        pairs.addAll(Arrays.asList(option.values));
                    }
                }
            }
        }).notifyUpdate(this,true);
    }
    public Options[] getOptions(){return options;}
    private Options getOptions(int index){
        int s=0; for(Options options:this.options){if(options!=null && index<(s+=options.values.length+1)){return options;}} return null;
    }
    public Script getScript(){return script;}
    private int onSelected(StringPair checked){
        StringPair last=null;
        for(StringPair pair:pairs){
            if(pair.getGroup()==checked.getGroup() && pair!=checked && pair.getChecked()==1){(last=pair).setChecked(0);}
        }
        return pairs.indexOf(last);
    }
    public void reset(){reset(true);}
    private void reset(boolean notify){
        for(Options op:options){
            if(op!=null){
                if(op.title.getType()==4){op.title.setValue(null);}
                op.title.setChecked(0);
                switch (op.values.length>0 ? op.values[0].getType() :-1){
                    case 1: onSelected(op.values[0].change()); break;
                    case 2:
                    case 3: for(StringPair pair:op.values){pair.setChecked(0);} break;
                    case 5: for(StringPair pair:op.values){pair.setKey(null); pair.setValue(null);}
                }
            }
        }
        if(notify){notifyItemRangeChanged(0,getItemCount());}
    }
    @Override
    public int getItemViewType(int position){return pairs.get(position).getType();}

    @NonNull
    @NotNull
    @Override
    public FilterSortAdapter.PairHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        LayoutInflater from=LayoutInflater.from(parent.getContext());
        switch (viewType){
            case -1: return new CheckedHolder(from.inflate(R.layout.header_group,parent,false),(v, position) -> {
                pairs.get(position).change();
                update_pairs();
            });
            case +0: return new CheckedHolder(from.inflate(R.layout.header_group_checked_sort,parent,false),(v, position) -> {
                pairs.get(position).change();
                notifyItemChanged(position);
            });
            case +1: return new CheckedHolder(from.inflate(R.layout.item_checked_radio, parent, false), (v, position) -> {
                notifyItemChanged(onSelected(pairs.get(position).change()));
                notifyItemChanged(position);
            });
            case +2: return new CheckedHolder(from.inflate(R.layout.item_checked_box, parent, false), (v, position) -> {
                pairs.get(position).change();
                notifyItemChanged(position);
            });
            case +3: return new CheckedHolder(from.inflate(R.layout.item_checked_box_3, parent, false), (v, position) -> {
                pairs.get(position).change();
                notifyItemChanged(position);
            });
            case +4: return new InputHolder(from.inflate(R.layout.item_input, parent, false));
            case +5: return new RangeInputHolder(from.inflate(R.layout.item_input_range,parent,false));
            default: throw new AssertionError("No such type of holder");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull FilterSortAdapter.PairHolder holder, int position) {
        holder.bind(pairs.get(position));
    }

    @Override
    public int getItemCount(){return pairs.size();}
    public static abstract class PairHolder extends RecyclerView.ViewHolder{
        public PairHolder(@NonNull @NotNull View itemView) {
            super(itemView);
        }
        public abstract void bind(StringPair pair);
    }

    public static final class CheckedHolder extends PairHolder{
        public CheckedHolder(@NonNull @NotNull View itemView,final HolderClickListener listener) {
            super(itemView); if(listener!=null){itemView.setOnClickListener(v->listener.onItemClick(v,getAbsoluteAdapterPosition()));}
        }
        public void bind(StringPair pair){
            setCheckedText(pair.getKey(),pair.getChecked());
        }
        public void setCheckedText(String text,int state){
            ((TextView)itemView).setText(text);
            if(itemView instanceof ThreeStatesTextView){
                ((ThreeStatesTextView)itemView).setState(state);
            }else{
                ((CheckedTextView)itemView).setChecked(state==1);
            }
        }
    }
    public static final class InputHolder extends PairHolder{
        TextView title;
        TextView input;
        StringPair pair;
        public InputHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            title=itemView.findViewById(android.R.id.title);
            input=itemView.findViewById(android.R.id.input);
            input.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    pair.setValue(s.length()==0? null : s.toString());
                }
            });
        }
        public void bind(StringPair pair){
            this.pair=pair;
            title.setText(pair.getKey());
            input.setText(pair.getValue());
        }
    }
    public static final class RangeInputHolder extends PairHolder{
        TextView start;
        TextView end;
        StringPair pair;
        public RangeInputHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            start=itemView.findViewById(R.id.start);
            end=itemView.findViewById(R.id.end);
            start.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    pair.setKey(s.length()==0? null : s.toString());
                }
            });
            end.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    pair.setValue(s.length()==0? null : s.toString());
                }
            });
        }
        public void bind(StringPair pair){
            this.pair=pair;
            start.setText(pair.getKey());
            end.setText(pair.getValue());
        }
    }

    public void changeOption(String name){
        for(StringPair pair:pairs){
            if(Objects.equals(name,pair.getKey())){
                pair.change();
            }
        }
    }
    public void selectOption(String name){setOption(name,1);}
    public void unselectOption(String name){setOption(name,0);}
    public void deselectOption(String name){setOption(name,-1);}
    private void setOption(String name, int value){
        if(name!=null){
            for(StringPair pair:pairs){
                if(Objects.equals(name,pair.getKey())){
                    pair.setChecked(value);
                }
            }
        }
    }
    public Spannable getClickableSpans(CharSequence text, ClickSpan.SpanClickListener listener){
        return getClickableSpans(text, getTitles(), listener);
    }
    public Set<String> getTitles(){
        Set<String> words=new HashSet<>(pairs.size());
        for(StringPair pair:pairs){words.add(pair.getKey());}
        return words;
    }
    public static void getTitles(FilterSortAdapter adapter, Set<String> words){
        if(adapter!=null){adapter.getTitles(words);}
    }
    public void getTitles(Set<String> words){
        for(StringPair pair:pairs){words.add(pair.getKey());}
    }

    public static Spannable getClickableSpans(CharSequence text, Collection<String> words, ClickSpan.SpanClickListener listener){
        Spannable spannable=text instanceof Spannable ? (Spannable) text : new SpannableStringBuilder(text);
        for(String word:words){
            if(word==null || word.length()==0){continue;}
            Matcher matcher=Pattern.compile(word.replaceAll("[\\\\\\.\\*\\+\\-\\?\\^\\$\\|\\(\\)\\[\\]\\{\\}]","\\\\W"), Pattern.CASE_INSENSITIVE).matcher(text);
            while(matcher.find()){
                int start=matcher.start(), end=matcher.end();
                ClickSpan[] spans=spannable.getSpans(start,end,ClickSpan.class);
                for(int i=0;i<(spans!=null ? spans.length : 0); i++){
                    if(end-start>spannable.getSpanEnd(spans[i])-spannable.getSpanStart(spans[i])){
                        spannable.removeSpan(spans[i]);
                    }
                }
                spannable.setSpan(new ClickSpan(word, listener), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return spannable;
    }
}
