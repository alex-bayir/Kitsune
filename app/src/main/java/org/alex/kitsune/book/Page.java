package org.alex.kitsune.book;

import com.alex.json.java.JSON;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Page {
    JSON.Object info;
    private Page(JSON.Object json){
        this.info=json;
    }
    public Page(Map<String,?> map){
        this(map.get("page") instanceof Number n?n.floatValue():0,(String)map.get("data"));
    }
    public Page(float num,String data){
        this(new JSON.Object().put("num",num).put("data",data));
    }
    public void setData(String data){info.put("data",data);}
    public String getData(){return info.get("data",info.getString("url"));}
    public float getNum(){return info.get("num",-1f);}
    public JSON.Object toJSON(){return info;}
    public static Page fromJSON(JSON.Object json){
        return json==null ? null : new Page(json);
    }
    public static JSON.Array<JSON.Object> toJSON(List<Page> pages){
        return pages==null ? null : new JSON.Array<>(pages.stream().map(Page::toJSON).collect(Collectors.toList()));}
    public static List<Page> fromJSON(List<JSON.Object> json){
        return json==null ? null : json.stream().map(Page::fromJSON).collect(Collectors.toList());
    }
    @NotNull
    @Override
    public String toString() {
        return toJSON().toString();
    }
}
