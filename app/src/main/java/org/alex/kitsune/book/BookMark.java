package org.alex.kitsune.book;

import com.alex.json.java.JSON;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.stream.Collectors;

public class BookMark{
    Chapter chapter;
    int page;
    long date;
    private static final String[] FN={"chapter","page","date"};
    private BookMark(Chapter chapter,int page,long date){
        this.chapter=chapter;
        this.page=page;
        this.date=date;
    }
    public BookMark(Chapter chapter,int page){this(chapter,page,System.currentTimeMillis());}
    public Chapter getChapter(){return chapter;}
    public int getPage(){return page;}
    public long getDate(){return date;}
    public JSON.Object toJSON(){
        return chapter==null? null : new JSON.Object().put(FN[0],chapter.toJSON()).put(FN[1],page).put(FN[2],date);
    }
    public static BookMark fromJSON(JSON.Object json){
        return json==null ? null : new BookMark(Chapter.fromJSON(json.getObject(FN[0])), json.getInt(FN[1]),json.getLong(FN[2]));
    }
    public static JSON.Array<JSON.Object> toJSON(List<BookMark> list){
        return list==null? null : new JSON.Array<>(list.stream().map(BookMark::toJSON).collect(Collectors.toList()));}
    public static List<BookMark> fromJSON(List<JSON.Object> json){
        return json==null ? null : json.stream().map(BookMark::fromJSON).collect(Collectors.toList());
    }
    public boolean equals(BookMark bookMark){
        return (bookMark!=null && (chapter.equals(bookMark.chapter) && page==bookMark.page));
    }
    @NotNull
    public String toString(){return toJSON().toString();}
}
