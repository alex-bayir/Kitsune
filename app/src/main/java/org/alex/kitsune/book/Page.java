package org.alex.kitsune.book;

import com.alex.json.java.JSON;
import java.util.List;
import java.util.stream.Collectors;

public class Page {
    JSON.Object info;
    String text;
    private Page(JSON.Object json){
        this.info=json;
    }
    public Page(float num,String url){
        this(new JSON.Object().put("num",num).put("url",url));
    }
    public Page(String text,float num){
        this(new JSON.Object().put("num",num));
        this.text=text;
    }
    public float getNum(){return info.get("num",-1f);}
    public String getUrl(){return info.getString("url");}
    public String getText(){return text;}
    public void setText(String text){this.text=text;}
    public JSON.Object toJSON(){return info;}
    public static Page fromJSON(JSON.Object json){
        return json==null ? null : new Page(json);
    }
    public static JSON.Array<JSON.Object> toJSON(List<Page> pages){
        return pages==null ? null : new JSON.Array<>(pages.stream().map(Page::toJSON).collect(Collectors.toList()));}
    public static List<Page> fromJSON(List<JSON.Object> json){
        return json==null ? null : json.stream().map(Page::fromJSON).collect(Collectors.toList());
    }
    @Override
    public String toString() {
        return toJSON().toString();
    }
}
