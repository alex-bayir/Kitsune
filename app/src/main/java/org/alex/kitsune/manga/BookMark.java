package org.alex.kitsune.manga;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class BookMark{
    Chapter chapter;
    int page;
    long date;
    private static final String[] FN={"chapter","page","date"};
    public BookMark(Chapter chapter,int page,long date){
        this.chapter=chapter;
        this.page=page;
        this.date=date;
    }
    public BookMark(Chapter chapter,int page){this(chapter,page,System.currentTimeMillis());}
    public Chapter getChapter(){return chapter;}
    public int getPage(){return page;}
    public long getDate(){return date;}
    public JSONObject toJSON() throws JSONException{
        return new JSONObject().put(FN[0],chapter!=null ? chapter.toJSON() : null).put(FN[1],page).put(FN[2],date);
    }
    public static BookMark fromJSON(JSONObject json) throws JSONException{
        return json==null?null:new BookMark(Chapter.fromJSON(json.optJSONObject(FN[0])), json.getInt(FN[1]),json.getLong(FN[2]));
    }
    public static JSONArray toJSON(List<BookMark> bookMarks) throws JSONException{
        JSONArray json=new JSONArray(); if(bookMarks!=null){for(BookMark bookMark : bookMarks){json.put(bookMark.toJSON());}}
    return json;}
    public static ArrayList<BookMark> fromJSON(JSONArray json) throws JSONException{
        ArrayList<BookMark> bookMarks=new ArrayList<>(json!=null?json.length():0); for(int i=0;i<(json!=null?json.length():0);i++){bookMarks.add(BookMark.fromJSON(json.optJSONObject(i)));}
    return bookMarks;}
    public boolean equals(BookMark bookMark){
        return (bookMark!=null && (chapter.equals(bookMark.chapter) && page==bookMark.page));
    }
    @NotNull
    public String toString(){try{return toJSON().toString();}catch (JSONException e){e.printStackTrace();} return "null";}
}
