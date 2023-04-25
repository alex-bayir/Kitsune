package org.alex.kitsune.book.search;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Map;

public class Options{
    final StringPair title;
    final StringPair[] sort;
    final StringPair[] values;
    public Options(String string, StringPair[] sort, @NotNull StringPair[] values, int mode){
        this.title=new StringPair(string,sort==null ? null : "direction"); mode=mode%5+1; this.values=values;
        for(StringPair to:this.values){to.setParams(mode,0,title.hashCode());}
        this.sort=sort;
    }
    public Options(String string, @NotNull Map<String,String> values, int mode){
        this(string,null,convert(values),mode);
    }
    public Options(String title,int mode){
        this.title=new StringPair(title,null,mode%5+1==4? 4 : -1);
        this.sort=null;
        this.values=mode%5+1==4?new StringPair[0]:new StringPair[]{new StringPair(null,null,mode%5+1)};
    }
    public Options(String string,String descending,String ascending, @NotNull Map<String,String> values, int mode){
        this(string,new StringPair[]{new StringPair(null,descending),new StringPair(null,ascending)},convert(values),mode);
    }
    public String getTitleSortSelected(){return sort[title.getChecked()].getValue();}
    public String getInput(){return title.getValue();}
    public String getInputRangeStart(){return values[0].getKey();}
    public String getInputRangeEnd(){return values[0].getValue();}
    public String[] getSelected(){return get(1);}
    public String[] getUnselected(){return get(0);}
    public String[] getDeselected(){return get(-1);}
    private String[] get(int value){
        ArrayList<String> array=new ArrayList<>(values.length);
        for(StringPair to:values){if(to.getChecked()==value){array.add(to.getValue());}}
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
        private String key;
        private String value;
        private int type;
        private int checked;
        private int group;

        public StringPair(String key, String value){
            this(key,value,value!=null ? 0:-1);
        }
        public StringPair(String key, String value, int type){
            this.key=key;
            this.value=value;
            this.type=type;
        }
        public String getKey(){return key;}
        public void setKey(String key){this.key=key;}
        public String getValue(){return value;}
        public void setValue(String value){this.value=value;}
        public int getType(){return type;}
        public void setChecked(int checked){this.checked=checked;}
        public int getChecked(){return checked;}
        public void setParams(int mode,int checked,int group){this.type=mode; this.checked=checked; this.group=group;}
        public int getGroup(){return group;}
        public StringPair change(){
            switch (type){
                case 5:
                case 4:
                case 1: checked=1; break;
                default:
                case 0:
                case 2: checked=(checked+1)%2; break;
                case 3: checked=(checked+2)%3-1; break;
            }
            return this;
        }
    }
}
