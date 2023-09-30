package org.alex.kitsune.book.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Options{
    final Header title;
    final Pair<?>[] values;
    public Options(String key,int mode){
        this.title=mode%5+1==5?new StringPairRange(key,key):new StringPairEditable(key,key);
        this.values=new Pair[0];
    }
    public Options(String title,@NotNull Map<String,String> values, int mode){
        this.title=new Header(title);
        this.values=convert(values,mode%5+1);
    }
    public Options(String title,String descending,String ascending, @NotNull Map<String,String> values, int mode){
        this.title=new HeaderSorted(title,title,descending,ascending);
        this.values=convert(values,mode%5+1);
    }
    public Options(Map<String,Object> map){
        this((int)map.getOrDefault("mode",0),map);
    }
    public Options(int mode,Map<String,Object> map){
        switch (mode){
            case 0-> {
                if(map.get("desc")!=null && map.get("asc")!=null){
                    this.title=new HeaderSorted((String) map.get("title"),(String) map.get("title"),(String) map.get("desc"),(String)map.get("asc"));
                }else{
                    this.title=new Header((String) map.get("title"));
                }
                this.values=convert((Map<String, String>) map.getOrDefault("values",Map.of()),mode%5+1);
            }
            case 1,2,3->{
                this.title=new Header((String) map.get("title"));
                this.values=convert((Map<String, String>) map.getOrDefault("values",Map.of()),mode%5+1);
            }
            default->{
                this.title=mode%5+1==5?
                        new StringPairRange((String) map.get("title"),(String) map.getOrDefault("key",(String) map.get("title")))
                        :
                        new StringPairEditable((String) map.get("title"),(String) map.getOrDefault("key",(String) map.get("title")));
                this.values=new Pair[0];
            }
        }
    }

    public static List<Options> convert(List<Map<String,Object>> list){
        return list==null? null:list.stream().map(Options::new).collect(Collectors.toList());
    }
    public String getTitleSortSelected(){return title.getValue();}
    public String getInput(){return title.getValue();}
    public String getInputRangeStart(){return title instanceof StringPairRange range?range.getLower():null;}
    public String getInputRangeEnd(){return title instanceof StringPairRange range?range.getUpper():null;}
    public String[] getUnselected(){return get(0);}
    public String[] getSelected(){return get(1);}
    public String[] getDeselected(){return get(2);}
    private String[] get(int value){
        return Arrays.stream(values).filter(pair->pair instanceof StringPair spair && spair.getState()==value).map(Pair::getValue).toArray(String[]::new);
    }
    private static Pair<String>[] convert(Map<String,String> map,int mode){
        if(map!=null){
            int count=0;
            Pair<String>[] pairs=new Pair[0];
            if(mode==1){
                pairs=new StringPairRadio[map.size()];
                for(Map.Entry<String,String> entry:map.entrySet()){
                    pairs[count++]=new StringPairRadio((StringPairRadio[])pairs,entry.getKey(), entry.getValue());
                }
            }else if(mode==2 || mode==3){
                pairs=new StringPair[map.size()];
                for(Map.Entry<String,String> entry:map.entrySet()){
                    pairs[count++]=new StringPair(mode,entry.getKey(), entry.getValue());
                }
            }
            return pairs;
        }
    return null;}

    public void reset(){
        title.reset();
        if(values.length>0 && values[0] instanceof StringPairRadio spr){
            spr.change();
        }else{
            for(Pair<?> pair:values){pair.reset();}
        }
    }
    public static abstract class Pair<T>{
        protected final String title;
        protected final String key;
        protected T value;
        private final int type;
        Pair(int type,String title,String key,T value){
            this.type=type;
            this.title=title;
            this.key=key;
            this.value=value;
        }
        public final int getType(){return type;}
        public final String getTitle(){return title;}
        public final String getKey(){return key;}
        public T getValue(){return value;}
        public void setValue(T value){}
        public Pair<T> change(){return this;}
        public void reset(){}
        public int getState(){return -1;}
        public boolean contains(@NonNull Pattern pattern){
            return title!=null && pattern.matcher(title).find();
        }

        @Override
        public boolean equals(@Nullable @org.jetbrains.annotations.Nullable Object obj) {
            return obj instanceof Pair<?> pair && this.type==pair.type && Objects.equals(this.key,pair.key) && Objects.equals(this.title,pair.title);
        }

        @Override
        public int hashCode() {
            return (type+(key!=null? key:"")+(title!=null?title:"")+(getValue()!=null?getValue():"")).hashCode();
        }
    }
    public static class Header extends Pair<String>{
        private boolean collapsed=false;
        protected Header(int type,String title,String key,String value){
            super(type,title,key,value);
        }
        Header(String title){
            this(-1,title,null,null);
        }
        public boolean isCollapsed(){return collapsed;}
        public void setCollapsed(boolean collapsed){this.collapsed=collapsed;}
        public void inverseCollapse(){setCollapsed(!collapsed);}
        @Override public int hashCode(){return super.hashCode()+(isCollapsed()?1:0);}
    }
    public static class HeaderSorted extends Header{
        final String[] sorts;
        private int selected=0;
        public HeaderSorted(String title, String key, String descending, String ascending){
            super(0,title,key,descending);
            sorts=new String[]{descending,ascending};
        }
        @Override public HeaderSorted change(){value=sorts[selected=(selected+1)%sorts.length]; return this;}
        @Override public void reset(){value=sorts[selected=0];}
        @Override public int getState(){return selected;}
        @Override public int hashCode(){return super.hashCode()+selected*2;}
    }
    public static class StringPair extends Pair<String>{
        protected int state=0;
        public StringPair(int states, String key, String value){
            super(states,key,key,value);
        }
        @Override public Pair<String> change() {
            state=(state+1)%getType();
            return this;
        }
        @Override public void reset(){state=0;}
        @Override public int getState(){return state;}
        public void setState(int state){
            this.state=Math.max(0,Math.min(state,Math.max(getType()-1,1)));
        }
    }
    public static class StringPairRadio extends StringPair {
        private final StringPairRadio[] group;
        private StringPairRadio(StringPairRadio[] group, String key, String value){
            super(1,key,value);
            this.group=group;
        }
        @Override public Pair<String> change() {
            StringPairRadio last=this;
            for(StringPairRadio pair:group){
                if(pair.state==1 && pair!=last){last=pair;}
                pair.state=pair==this?1:0;
            }
            return last;
        }
    }

    public static class StringPairEditable extends Header{
        private StringPairEditable(String key, String value){super(4,key,key,value);}
        @Override public void setValue(String value){this.value=value;}
        @Override public void reset(){setValue(null);}
    }
    public static class StringPairRange extends Header{
        String lower,upper;
        public StringPairRange(String title, String key){
            super(5,title,key,":");
        }

        public void setLower(String lower){
            initValue(this.lower=lower,upper);
        }
        public void setUpper(String upper){
            initValue(lower,this.upper=upper);
        }
        public String getLower(){return lower;}
        public String getUpper(){return upper;}
        @Override public void reset(){initValue(lower=null,upper=null);}
        private void initValue(String lower,String upper){
            value=(lower==null?"":lower)+":"+(upper==null?"":upper);
        }
    }

}
