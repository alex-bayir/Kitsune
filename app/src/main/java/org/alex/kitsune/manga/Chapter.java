package org.alex.kitsune.manga;

import android.content.Context;
import org.alex.kitsune.R;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Chapter {
    int id;
    int vol;
    float num;
    String name;
    long date;
    String info;
    ArrayList<Page> pages;
    private static final String[] FN={"id","vol","num","name","date","pages"};

    public Chapter(int id,int vol,float num,String name,long date){this(id,vol,num,name,date,null,null);}
    public Chapter(int id,int vol,float num,String name,long date,String info){this(id,vol,num,name,date,null, info);}
    public Chapter(int id,int vol,float num,String name,long date,ArrayList<Page> pages){this(id,vol,num,name,date,pages,null);}
    public Chapter(int id,int vol,float num,String name,long date,ArrayList<Page> pages, String info){
        this.id=id;
        this.vol=vol;
        this.num=num;
        this.name=(name==null || "null".equals(name)) ? "" : name;
        this.date=date;
        this.pages=pages;
        this.info=info;
    }

    public Page getPage(int page){return (pages!=null && page<pages.size()) ? pages.get(page) : null;}

    public int getId(){return id;}
    public int getVol(){return vol;}
    public float getNum(){return num;}
    public String getName(){return name;}
    public long getDate(){return date;}
    public ArrayList<Page> getPages(){return pages;}
    public String getInfo(){return info;}

    public JSONObject toJSON() throws JSONException {
        return new JSONObject().put(FN[0],id).put(FN[1],vol).put(FN[2],num).put(FN[3],name).put(FN[4],date).put(FN[5],Page.toJSON(pages));
    }
    public static Chapter fromJSON(JSONObject json) throws JSONException{
        return json==null?null:new Chapter(json.getInt(FN[0]),json.getInt(FN[1]),(float)json.getDouble(FN[2]), json.getString(FN[3]), json.getLong(FN[4]), Page.fromJSON(json.getJSONArray(FN[5])));
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
}
