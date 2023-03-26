package org.alex.kitsune.book.search;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Map;

public class Options{
    final StringPair title;
    final StringPair[] sort;
    final StringPair[] values;
    public Options(String string, StringPair[] sort, @NotNull StringPair[] values, int mode){
        this.title=new StringPair(string,sort==null ? null : "direction"); mode=mode%3+1; this.values=values;
        for(StringPair to:this.values){to.setParams(mode,0,title.hashCode());}
        this.sort=sort;
    }
    public Options(String string, @NotNull Map<String,String> values, int mode){
        this(string,null,convert(values),mode);
    }
    public Options(String string,String descending,String ascending, @NotNull Map<String,String> values, int mode){
        this(string,new StringPair[]{new StringPair(null,descending),new StringPair(null,ascending)},convert(values),mode);
    }
    public String getTitleSortSelected(){return sort[title.getValue()].getUrlParam();}
    public String[] getSelected(){return get(1);}
    public String[] getUnselected(){return get(0);}
    public String[] getDeselected(){return get(-1);}
    private String[] get(int value){
        ArrayList<String> array=new ArrayList<>(values.length);
        for(StringPair to:values){if(to.getValue()==value){array.add(to.getUrlParam());}}
        return array.toArray(new String[0]);
    }
    private static StringPair[] convert(Map<String,String> map){
        if(map!=null){
            ArrayList<StringPair> pairs=new ArrayList<>(map.size());
            for(Map.Entry<String,String> entry:map.entrySet()){
                pairs.add(new StringPair(entry.getKey(), entry.getValue()));
            }
            return pairs.toArray(new StringPair[0]);
        }
    return null;}

    public static final class StringPair {
        private final String name;
        private final String urlParam;
        private int type;
        private int value;
        private int group;

        public StringPair(String name, String urlParam){
            this.name=name;
            this.urlParam=urlParam;
            this.type=urlParam!=null ? 0:-1;
        }
        public String getName(){return name;}
        public String getUrlParam(){return urlParam;}
        public int getType(){return type;}
        public void setValue(int value){this.value=value;}
        public int getValue(){return value;}
        public void setParams(int mode,int value,int group){this.type=mode; this.value=value; this.group=group;}
        public int getGroup(){return group;}
        public StringPair change(){
            switch (type){
                case 1: value=1; break;
                default:
                case 0:
                case 2: value=(value+1)%2; break;
                case 3: value=(value+2)%3-1; break;
            }
            return this;
        }
    }
}
