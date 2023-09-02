package org.alex.kitsune.book.search;

import android.text.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.commons.ClickSpan;
import org.alex.kitsune.commons.DiffCallback;
import org.alex.kitsune.commons.HolderClickListener;
import com.alex.threestates.ThreeStatesTextView;
import org.alex.kitsune.scripts.Script;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;


public final class FilterSortAdapter extends RecyclerView.Adapter<FilterSortAdapter.PairHolder> {

    private final Script script;
    private final Options[] options;
    private final ArrayList<Options.Pair<?>> pairs=new ArrayList<>(200);
    private final DiffCallback<Options.Pair<?>> notify=new DiffCallback<>();
    private String search=null;

    public FilterSortAdapter(Script script,List<Options> options){
        this(script,options!=null ? options.stream().filter(Objects::nonNull).toArray(Options[]::new) : new Options[0]);
    }
    public FilterSortAdapter(Script script,Options... options){
        for(Options option : options){if(option==null){throw new IllegalArgumentException("Options cannot be null");}}
        this.options=options;
        this.script=script;
        update_pairs(search);
        reset(false);
    }

    public Callback<String> getSearchCallback(){return this::update_pairs;}
    private static final Pattern check_is_simple_words=Pattern.compile("^[\\w\\s]+$");
    public void update_pairs(String search){
        try{
            if(search!=null && search.length()>0 && check_is_simple_words.matcher(search).find()){search="(?i)"+search;}
            Pattern pattern=search!=null && search.length()>0 ? Pattern.compile(search):null;
            this.search=search;
            List<Options.Pair<?>> old=new ArrayList<>(pairs);
            pairs.clear();
            for(Options option:options){
                if(pattern==null){
                    pairs.add(option.title);
                    if(!option.title.isCollapsed()){pairs.addAll(Arrays.asList(option.values));}
                }else{
                    List<Options.Pair<?>> p=Arrays.stream(option.values).filter(pair->pair.contains(pattern)).collect(Collectors.toList());
                    if(p.size()>0 || option.title.contains(pattern)){pairs.add(option.title); pairs.addAll(p);}
                }
            }
            notify.init(old,pairs,false).notifyUpdate(this);
        }catch (PatternSyntaxException ignore){}
    }
    public Options[] getOptions(){return options;}
    private Options getOptions(int index){
        int s=0; for(Options options:this.options){if(index<(s+=options.values.length+1)){return options;}} return null;
    }
    public Script getScript(){return script;}

    public void reset(){reset(true);}
    private void reset(boolean notify){
        for(Options op:options){
            op.reset();
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
        return switch (viewType) {
            case -1 -> new CheckedHolder(from.inflate(R.layout.header_group, parent, false), (v, position) -> {
                if (pairs.get(position) instanceof Options.Header h) {
                    h.inverseCollapse();
                    update_pairs(search);
                }
            });
            case +0 -> new CheckedHolder(from.inflate(R.layout.header_group_checked_sort, parent, false), (v, position) -> {
                        if (pairs.get(position) instanceof Options.HeaderSorted h) {
                            if (v.getId() == android.R.id.checkbox) {
                                h.change();
                                notifyItemChanged(position);
                            } else {
                                h.inverseCollapse();
                                update_pairs(search);
                            }
                        }
                    });
            case +1 -> new CheckedHolder(from.inflate(R.layout.item_checked_radio, parent, false), (v, position) -> {
                notifyItemChanged(pairs.indexOf(pairs.get(position).change()));
                notifyItemChanged(position);
            });
            case +2 -> new CheckedHolder(from.inflate(R.layout.item_checked_box, parent, false), (v, position) -> {
                pairs.get(position).change();
                notifyItemChanged(position);
            });
            case +3 -> new CheckedHolder(from.inflate(R.layout.item_checked_box_3, parent, false), (v, position) -> {
                pairs.get(position).change();
                notifyItemChanged(position);
            });
            case +4 -> new InputHolder(from.inflate(R.layout.item_input, parent, false));
            case +5 -> new RangeInputHolder(from.inflate(R.layout.item_input_range, parent, false), (v, position) -> {
                if (pairs.get(position) instanceof Options.StringPairRange h) {
                    h.inverseCollapse();
                    notifyItemChanged(position);
                }
            });
            default -> throw new AssertionError("No such type of holder");
        };
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
        public abstract void bind(Options.Pair<?> pair);
    }

    public static final class CheckedHolder extends PairHolder{
        TextView title;
        public CheckedHolder(@NonNull @NotNull View itemView,final HolderClickListener listener) {
            super(itemView);
            title=itemView.findViewById(android.R.id.title);
            if(listener!=null){
                itemView.setOnClickListener(v->listener.onItemClick(v,getAbsoluteAdapterPosition()));
                View view=itemView.findViewById(android.R.id.checkbox);
                if(view!=null){view.setOnClickListener(v->listener.onItemClick(v,getAbsoluteAdapterPosition()));}
            }
        }
        public void bind(Options.Pair<?> pair){
            setCheckedText(pair.getTitle(),pair.getState());
        }
        public void setCheckedText(String text,int state){
            title.setText(text);
            if(title instanceof ThreeStatesTextView){
                ((ThreeStatesTextView)title).setState((state+1)%3-1);
            }else{
                ((CheckedTextView)title).setChecked(state==1);
            }
        }
    }
    public static final class InputHolder extends PairHolder{
        TextView title;
        TextView input;
        Options.StringPairEditable pair;
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
        public void bind(Options.Pair<?> pair){
            if(pair instanceof Options.StringPairEditable p){
                this.pair=p;
                title.setText(this.pair.getTitle());
                input.setText(this.pair.getValue());
            }
        }
    }
    public static final class RangeInputHolder extends PairHolder{
        TextView title;
        EditText start, end;
        Options.StringPairRange pair;
        View container;
        public RangeInputHolder(@NonNull @NotNull View itemView,final HolderClickListener title_listener) {
            super(itemView);
            title=itemView.findViewById(android.R.id.title);
            container=itemView.findViewById(R.id.container);
            start=itemView.findViewById(R.id.start);
            end=itemView.findViewById(R.id.end);
            start.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s){pair.setLower(s.length()==0? null : s.toString());}
            });
            end.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    pair.setUpper(s.length()==0? null : s.toString());
                }
            });
            if(title_listener!=null){
                title.setOnClickListener(v->title_listener.onItemClick(v,getAbsoluteAdapterPosition()));
            }
        }
        public void bind(Options.Pair<?> pair){
            if(pair instanceof Options.StringPairRange p){
                this.pair=p;
                title.setText(this.pair.getTitle());
                start.setText(this.pair.getLower());
                end.setText(this.pair.getUpper());
                container.setVisibility(this.pair.isCollapsed()?View.GONE:View.VISIBLE);
            }
        }
    }
    public void selectOption(String name){setOption(name,1);}
    public void unselectOption(String name){setOption(name,0);}
    public void deselectOption(String name){setOption(name,-1);}
    private void setOption(String name, int value){
        if(name!=null){
            pairs.stream()
                    .filter(pair -> pair instanceof Options.StringPair && Objects.equals(pair.getKey(),name))
                    .map(pair->(Options.StringPair)pair)
                    .forEachOrdered(pair->pair.setState(value));

        }
    }
    public Spannable getClickableSpans(CharSequence text, ClickSpan.SpanClickListener listener){
        return getClickableSpans(text, getTitles(), listener);
    }
    public Set<String> getTitles(){
        return getTitles(new HashSet<>(pairs.size()));
    }
    public static void getTitles(FilterSortAdapter adapter, Set<String> words){
        if(adapter!=null){adapter.getTitles(words);}
    }
    public Set<String> getTitles(Set<String> words){
        pairs.stream().filter(pair->pair instanceof Options.StringPair && pair.getKey()!=null).forEachOrdered(pair->words.add(pair.getKey()));
        return words;
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
