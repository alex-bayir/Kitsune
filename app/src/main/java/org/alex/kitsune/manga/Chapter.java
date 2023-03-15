package org.alex.kitsune.manga;

import android.content.Context;
import org.alex.kitsune.R;
import org.alex.json.JSON;
import org.alex.kitsune.utils.Utils;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static org.alex.json.JSON.filter;

public class Chapter{
    JSON.Object info;
    private static final String[] FN={"vol","num","name","date","pages"};
    public Chapter(JSON.Object info,List<Page> pages){
        this.info=info;
        this.info.put("pages",pages);
    }
    public Chapter(int vol,float num,String name,long date,List<Page> pages,JSON.Object additional){
        info=new JSON.Object();
        if(additional!=null){info.putAll(additional);}
        info.put("vol",vol);
        info.put("num",num);
        info.put("name",Utils.unescape_unicodes((name==null || "null".equals(name)) ? "" : name));
        info.put("date",date);
        info.put("pages",pages);
    }
    public Chapter(int vol,float num,String name,long date,JSON.Object additional){
        this(vol,num,name,date,null,additional);
    }
    public Chapter(int vol,float num,String name,long date,Map<String,Object> additional){
        this(vol,num,name,date,new JSON.Object(additional));
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
    public List<Page> getPages(){return info.get("pages") instanceof List<?> pages ? filter(pages, Page.class):null;}
    public List<Page> setPages(List<Page> pages){
        info.put("pages",pages);
        return pages;
    }
    public JSON.Object getInfo(){return info;}
    public String getTranslator(){return info.getString("translator");}

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
        return json==null ? null : json.stream().map(Chapter::fromJSON).collect(Collectors.toList());
    }

    public boolean equals(Object obj){return obj instanceof Chapter chapter && equals(chapter);}
    public boolean equals(Chapter chapter){
        return this==chapter || (chapter!=null && (this.getNum()==chapter.getNum() && this.getVol()==chapter.getVol()));
    }
    @NotNull
    public String toString(){return toJSON().toString();}
    private static final java.text.DecimalFormat f=new java.text.DecimalFormat("#.##");
    public String text(Context context){return context.getString(R.string.Volume)+" "+getVol()+" "+context.getString(R.string.Chapter)+" "+f.format(getNum()).replace(',','.')+(getName().length()>0 ? " - "+getName() : "");}
    public int countPages(){return getPages()==null ? 0: getPages().size();}
}
