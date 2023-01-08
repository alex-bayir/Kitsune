package org.alex.kitsune.manga;

import android.content.Context;
import org.alex.kitsune.R;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Chapter {
    int id;
    int vol;
    float num;
    String name;
    long date;
    JSONObject info;
    ArrayList<Page> pages;
    private static final String[] FN={"id","vol","num","name","date","pages","info"};
    public Chapter(int id,int vol,float num,String name,long date,ArrayList<Page> pages,JSONObject additional){
        this.id=id;
        this.vol=vol;
        this.num=num;
        this.name=(name==null || "null".equals(name)) ? "" : name;
        this.date=date;
        this.pages=pages;
        this.info=additional;
    }
    public Chapter(int id, int vol, float num, String name, long date){
        this(id, vol, num, name, date, null, null);
    }
    public Chapter(JSONObject json,String id,String vol,String num,String name,String date,String format){
        this(json.optInt(id),json.optInt(vol),json.optInt(num),json.optString(name),parse(json.optString(date),format));
    }
    public Chapter(JSONObject json,String id,String vol,String num,String name,String date){
        this(json.optInt(id),json.optInt(vol),json.optInt(num),json.optString(name),json.optLong(date));
    }
    public Chapter(JSONObject json,String id,String vol,String num,String name,String date,int scale){
        this(json.optInt(id),json.optInt(vol),json.optInt(num),json.optString(name),json.optLong(date)*scale);
    }
    public Chapter(int id, int vol, float num, String name, long date, Map<String,Object> map){
        this(id, vol, num, name, date, null, map.size()>0?new JSONObject(map):null);
    }
    public Chapter(JSONObject json,String id,String vol,String num,String name,String date,String format,Map<String,Object> map){
        this(json.optInt(id),json.optInt(vol),json.optInt(num),json.optString(name),parse(json.optString(date),format),map);
    }
    public Chapter(JSONObject json,String id,String vol,String num,String name,String date,Map<String,Object> map){
        this(json.optInt(id),json.optInt(vol),json.optInt(num),json.optString(name),json.optLong(date),map);
    }
    public Page getPage(int page){return (pages!=null && page<pages.size()) ? pages.get(page) : null;}
    public int getId(){return id;}
    public int getVol(){return vol;}
    public float getNum(){return num;}
    public String getName(){return name;}
    public long getDate(){return date;}
    public ArrayList<Page> getPages(){return pages;}
    public JSONObject getInfo(){return info;}
    public String getTranslater(){return info!=null?info.optString("translater"):null;}

    public JSONObject toJSON() throws JSONException {
        return new JSONObject().put(FN[0],id).put(FN[1],vol).put(FN[2],num).put(FN[3],name).put(FN[4],date).put(FN[5],Page.toJSON(pages)).put(FN[6],info);
    }
    public static Chapter fromJSON(JSONObject json) throws JSONException{
        return json==null?null:new Chapter(json.getInt(FN[0]),json.getInt(FN[1]),(float)json.getDouble(FN[2]), json.getString(FN[3]), json.getLong(FN[4]), Page.fromJSON(json.getJSONArray(FN[5])), json.optJSONObject(FN[6]));
    }
    public static JSONArray toJSON(List<Chapter> chapters) throws JSONException{
        JSONArray json=new JSONArray(); if(chapters!=null){for(Chapter chapter : chapters){json.put(chapter.toJSON());}}
    return json;}
    public static ArrayList<Chapter> fromJSON(JSONArray json) throws JSONException{
        ArrayList<Chapter> chapters=new ArrayList<>(json!=null?json.length():0); for(int i=0;i<(json!=null?json.length():0);i++){chapters.add(Chapter.fromJSON(json.getJSONObject(i)));}
    return chapters;}

    public boolean equals(Object obj){return obj instanceof Chapter && equals((Chapter) obj);}
    public boolean equals(Chapter chapter){
        return this==chapter || (chapter!=null && (this.id==chapter.id && this.num==chapter.num && this.vol==chapter.vol));
    }
    @NotNull
    public String toString(){try{return toJSON().toString();}catch(JSONException e){e.printStackTrace(); return "null";}}
    private static final java.text.DecimalFormat f=new java.text.DecimalFormat("#.##");
    public String text(Context context){return context.getString(R.string.Volume)+" "+vol+" "+context.getString(R.string.Chapter)+" "+f.format(num)+(name.length()>0 ? " - "+name : "");}
    public int countPages(){return pages!=null ? pages.size() : 0;}
    private static long parse(String date,String format){
        try{return java.util.Objects.requireNonNull(new java.text.SimpleDateFormat(format,java.util.Locale.US).parse(date)).getTime();}catch (Exception e){return System.currentTimeMillis();}
    }
}
