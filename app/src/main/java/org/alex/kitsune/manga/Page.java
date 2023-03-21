package org.alex.kitsune.manga;

import org.alex.json.JSON;
import java.util.List;
import java.util.stream.Collectors;

public class Page {
    float num;
    String url;
    public Page(float num,String url){
        this.num=num;
        this.url=url;
    }
    public float getNum(){return num;}
    public String getUrl(){return url;}
    public JSON.Object toJSON(){
        return new JSON.Object().put("num",num).put("url",url);
    }
    public static Page fromJSON(JSON.Object json){
        return json==null ? null : new Page(json.getFloat("num"), json.getString("url"));
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
