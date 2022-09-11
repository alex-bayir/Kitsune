package org.alex.kitsune.manga;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Page {
    int id;
    float num;
    String url;
    private static final String[] FN={"id","num","url"};

    public Page(int id,float num,String url){
        this.id=id;
        this.num=num;
        this.url=url;
    }
    public int getId(){return id;}
    public float getNum(){return num;}
    public String getUrl(){return url;}

    public JSONObject toJSON() throws JSONException {
        return new JSONObject().put(FN[0],id).put(FN[1],num).put(FN[2],url);
    }
    public static Page fromJSON(JSONObject json) throws JSONException{
        return new Page(json.getInt(FN[0]), (float)json.getDouble(FN[1]), json.getString(FN[2]));
    }
    public static JSONArray toJSON(List<Page> pages) throws JSONException{
        JSONArray json=new JSONArray(); if(pages!=null){for(Page page : pages){json.put(page.toJSON());}}
    return json;}
    public static ArrayList<Page> fromJSON(JSONArray json) throws JSONException{
        ArrayList<Page> pages=new ArrayList<>();
        if(json!=null){for(int i=0;i<json.length();i++){pages.add(Page.fromJSON(json.getJSONObject(i)));}}
    return pages;}
}
