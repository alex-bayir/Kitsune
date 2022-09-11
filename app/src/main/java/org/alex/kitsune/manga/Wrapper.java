package org.alex.kitsune.manga;

import org.alex.kitsune.utils.NetworkUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class Wrapper{
    public static String loadPage(String url) throws IOException{
        return NetworkUtils.getString(url);
    }
    public static Document loadDocument(String url) throws IOException{
        return NetworkUtils.getDocument(url);
    }
    public static long parseDate(String date,String format){
        try{
            return java.util.Objects.requireNonNull(new SimpleDateFormat(format,java.util.Locale.US).parse(date)).getTime();
        }catch (Exception e){
            return System.currentTimeMillis();
        }
    }

    public static String attr(Element element, String attr, String defValue){return element!=null ? element.attr(attr) : defValue;}
    public static String attr(Element element, String attr){return attr(element, attr,null);}
    public static String text(Element element, String defText){return element!=null ? element.text() : defText;}
    public static String text(Element element){return text(element,null);}
    public static String attr(Elements elements, String attr, String defValue){return ((elements!=null && elements.size()>0) ? elements.attr(attr) : defValue);}
    public static String attr(Elements elements, String attr){return attr(elements, attr,null);}
    public static String text(Elements elements, String defText){return ((elements!=null && elements.size()>0) ? elements.text() : defText);}
    public static String text(Elements elements){return text(elements,null);}

    public final String url;
    public int id;
    public String name;
    public String name_alt;
    public String author;
    public String author_url;

    public String genres;
    public double rating;
    public int status;
    public String description;
    public String thumbnail;
    public String url_web;

    public final ArrayList<Chapter> chapters;
    public final ArrayList<Wrapper> similar;

    public Wrapper(String url, int id, String name, String name_alt, String author, String author_url, String genres, double rating, String status, String description, String thumbnail, String url_web, ArrayList<Chapter> chapters, ArrayList<Wrapper> similar){
        this(url, id, name, name_alt, author, author_url, genres, rating, Manga.getStatus(status), description, thumbnail, url_web, chapters, similar);
    }
    public Wrapper(String url, int id, String name, String name_alt, String author, String author_url, String genres, double rating, int status, String description, String thumbnail, String url_web, ArrayList<Chapter> chapters, ArrayList<Wrapper> similar){
        this.url=url;
        this.id=id;
        this.name=name;
        this.name_alt=name_alt;
        this.author=author;
        this.author_url=author_url;

        this.genres=genres;
        this.rating=rating;
        this.status=status;
        this.description=description;
        this.thumbnail=thumbnail;
        this.url_web=url_web;

        this.chapters=chapters!=null ? chapters : new ArrayList<>();
        this.similar=similar!=null ? similar : new ArrayList<>();
    }
}

