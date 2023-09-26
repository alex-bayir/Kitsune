package org.alex.kitsune.book;

import android.content.Context;
import org.alex.kitsune.R;
import com.alex.json.java.JSON;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import static com.alex.json.java.JSON.filter;

public class Chapter{
    JSON.Object info;
    private Chapter(JSON.Object info,List<Page> pages){
        this.info=info.put("pages",pages);
    }
    public Chapter(Map<String,?> map){
        this.info=map instanceof JSON.Object obj ? obj : new JSON.Object(map);
    }
    public Page getPage(int page){
        return getPage(page,getPages());
    }
    public static Page getPage(int page,List<Page> pages){
        return (pages!=null && page>=0 && page<pages.size()) ? pages.get(page) : null;
    }
    public int getVol(){return info.get("vol",-1);}
    public float getNum(){return info.get("num",-1f);}
    public String getName(){return info.get("name","");}
    public long getDate(){return info.get("date",0L);}
    public boolean isClose(){return info.get("close",false);}
    public List<Page> getPages(){return info.get("pages") instanceof List<?> pages ? filter(pages, Page.class):null;}
    public List<Page> setPages(List<Page> pages){
        info.put("pages",pages); return pages;
    }
    public JSON.Object getInfo(){return info;}
    public String getTranslator(){return getTranslators()==null? null:getTranslators().keySet().stream().findFirst().orElse(null);}
    public Map<String,String> getTranslators(){
        return info.get("translators") instanceof Map<?,?> m ? (Map<String, String>) m: null;
    }

    public static List<Chapter> convert(List<Map<String,?>> list){
        return list==null? null: list.stream().filter(Objects::nonNull).map(Chapter::new).collect(Collectors.toList());
    }
    public JSON.Object toJSON(){
        return new JSON.Object(info).put("pages",Page.toJSON(getPages()));
    }
    public static Chapter fromJSON(JSON.Object json){
        return json==null ? null : new Chapter(json, Page.fromJSON(filter(json.getArray("pages"),JSON.Object.class)));
    }
    public static JSON.Array<JSON.Object> toJSON(List<Chapter> chapters){
        return chapters==null ? null : new JSON.Array<>(chapters.stream().map(Chapter::toJSON).collect(Collectors.toList()));
    }
    public static List<Chapter> fromJSON(List<JSON.Object> json){
        return json==null ? null : json.stream().map(Chapter::fromJSON).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public boolean equals(Object obj){return obj instanceof Chapter chapter && equals(chapter);}
    public boolean equals(Chapter chapter){
        return this==chapter || (chapter!=null && (this.getNum()==chapter.getNum() && this.getVol()==chapter.getVol()));
    }
    @NotNull
    public String toString(){return toJSON().toString();}
    private static final java.text.DecimalFormat f=new java.text.DecimalFormat("#.##");
    public String text(Context context){return (getVol()>0?context.getString(R.string.Volume)+" "+getVol()+" ":"")+context.getString(R.string.Chapter)+" "+f.format(getNum()).replace(',','.')+(getName().length()>0 ? " - "+getName() : "");}
    public int countPages(){return getPages()==null ? 0: getPages().size();}
}
