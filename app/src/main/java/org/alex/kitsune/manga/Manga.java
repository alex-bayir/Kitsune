package org.alex.kitsune.manga;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import androidx.annotation.Nullable;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.ClickSpan;
import org.alex.kitsune.commons.ListSet;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.manga.search.FilterSortAdapter;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.services.LoadService;
import org.alex.kitsune.utils.LoadTask;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Utils;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.util.*;


public abstract class Manga {
    protected final String url;
    protected int id;
    protected String name;
    protected String name_alt;
    protected String author;
    protected String author_url;

    protected String genres;
    protected double rating;
    protected int status;
    protected String description;
    protected String thumbnail;
    protected String url_web;

    protected final ArrayList<Chapter> chapters;

    protected final ArrayList<BookMark> bookMarks;
    protected BookMark history;

    protected String CategoryFavorite;

    private int lastSize;

    private String dir;

    private long lastTimeSave;
    private long lastTimeFavorite;
    private int lastMaxChapters;
    private boolean updated=false;
    protected final ListSet<Manga> similar=new ListSet<>(new ArrayList<>(10));

    public static final int Status_None=0;
    public static final int Status_Ongoing=1;
    public static final int Status_Released=2;
    public static final String[] FN={"Source","url","id","name","name_alt","author","author url","genres","rating","status","description","thumbnail","url_web","chapters","bookmarks","history","CategoryFavorite","lastSize","dir","lastTimeSave","lastTimeFavorite","lastMaxChapters"};

    public Manga(String url, int id, String name, String name_alt, String author,String author_url, String genres, double rating, int status, String description, String thumbnail, String url_web, ArrayList<Chapter> chapters, ArrayList<BookMark> bookMarks, BookMark history, String CategoryFavorite, int lastSize, String dir, long lastTimeSave, long lastTimeFavorite, int lastMaxChapters){
        this.url=url.replace("http://","https://");
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
        this.url_web=url_web!=null?url_web:url;

        this.chapters=chapters!=null ? chapters : new ArrayList<>();
        this.bookMarks=bookMarks!=null ? bookMarks : new ArrayList<>();
        this.history=history;

        this.CategoryFavorite=CategoryFavorite;

        this.lastSize=lastSize==-1 ? this.chapters.size() : lastSize;

        this.dir=dir;
        this.lastTimeSave=lastTimeSave;
        this.lastTimeFavorite=lastTimeFavorite;
        this.lastMaxChapters=lastMaxChapters;
    }
    public Manga clone(){
        return Objects.requireNonNull(newInstance(getProviderName(), url, id, name, name_alt, author, author_url, genres, rating, status, description, thumbnail, url_web, new ArrayList<>(chapters), new ArrayList<>(bookMarks), history, CategoryFavorite, lastSize, dir, lastTimeSave, lastTimeFavorite,lastMaxChapters));
    }

    public static Manga newInstance(String providerName,String url,int id,String name,String name_alt,String author,String author_url,String genres,double rating,int status,String description,String thumbnail,String url_web,ArrayList<Chapter> chapters,ArrayList<BookMark> bookMarks,BookMark history,String CategoryFavorite,int lastSize,String dir,long lastTimeSave,long lastTimeFavorite,int lastMaxChapters){
        return Manga_Scripted.newInstance(providerName,url,id,name,name_alt,author,author_url,genres,rating,status,description,thumbnail,url_web,chapters,bookMarks,history,CategoryFavorite,lastSize,dir,lastTimeSave,lastTimeFavorite,lastMaxChapters);
    }

    public abstract String getProvider();
    public abstract String getProviderName();
    public final String getStatus(){return getStatus(this.status);}
    public static String getStatus(int status){
        switch(status){
            case Status_Ongoing: return "Ongoing";
            case Status_Released: return "Completed";
            default: return "None";
        }
    }
    public final String getStatus(Context context){return getStatus(this.status,context);}
    public static String getStatus(int status,Context context){
        switch(status){
            case Status_Ongoing: return context.getString(R.string.Ongoing);
            case Status_Released: return context.getString(R.string.Completed);
            default: return context.getString(R.string.None);
        }
    }
    public static int getStatus(String status){
        switch (status!=null ? status.toLowerCase() : "none"){
            case "продолжается":
            case "ongoing": return Status_Ongoing;
            case "cингл":
            case "single":
            case "завершен":
            case "completed":
            case "released": return Status_Released;
            default:
            case "none": return Status_None;
        }
    }
    public abstract boolean update() throws Exception;

    public final ArrayList<Page> getPages(int chapter) throws IOException, JSONException{
        return getPages(chapters.get(chapter));
    }
    public final ArrayList<Page> getPagesE(Chapter chapter){
        try{return getPages(chapter);}catch(Exception e){Logs.saveLog(e); return null;}
    }
    public abstract ArrayList<Page> getPages(Chapter chapter) throws IOException, JSONException;
    protected final void updateSimilar(Set<Manga> mangas){
        mangas.removeAll(Collections.singleton(null));
        if(mangas.size()!=0){
            similar.clear();
            similar.addAll(mangas);
        }
    }
    protected abstract HashSet<Manga> loadSimilar() throws Exception;
    public final void loadSimilar(Callback<Set<Manga>> callback,Callback<Throwable> error){
        if(similar.size()==0){
            new Thread(() -> {
                try{
                    updateSimilar(loadSimilar());
                    new Handler(Looper.getMainLooper()){@Override public void handleMessage(Message msg){callback.call(getSimilar());}}.sendMessage(new Message());
                }catch(Exception e){
                    new Handler(Looper.getMainLooper()){@Override public void handleMessage(Message msg){error.call(e);}}.sendMessage(new Message());
                }
            }).start();
        }else{
            callback.call(similar);
        }
    }



    public final String getUrl(){return url;}
    public String getUrl_WEB(){return url_web;}
    public final int getId(){return id;}
    public final String getName(){return name;}
    public final String getNameAlt(){return name_alt;}
    public final String getAnyName(){return getAnyName(false);}
    public final String getAnyName(boolean en){return en ? name!=null ? name : name_alt : name_alt!=null ? name_alt : name;}
    public final String getAuthor(){return author;}
    public final String getAuthorUrl(){return author_url;}
    public final String getGenres(){return genres;}
    public final double getRating(){return rating;}
    public final String getDescription(){return description;}
    public final CharSequence getDescription(int flags){return getDescription(flags,null);}
    public final CharSequence getDescription(int flags, String def){return description!=null && description.length()>0 ? Html.fromHtml(description, flags) : def;}
    public final BookMark getHistory(){return history;}
    public final ArrayList<Chapter> getChapters(){return chapters;}
    public final ArrayList<BookMark> getBookMarks(){return bookMarks;}
    public final ListSet<Manga> getSimilar(){return similar;}
    @Override public final int hashCode(){return url.hashCode();}
    @Override public final boolean equals(@Nullable Object obj){return obj instanceof Manga && hashCode()==obj.hashCode();}

    public final String setDir(String dir){return this.dir=dir+(dir.endsWith("/") ? "" :"/")+hashCode();}
    public final String getDir(){return dir;}
    public final String getCoverPath(){return getDir()+"/card";}
    public final String getInfoPath(){return getDir()+"/summary";}
    public final String getPagesPath(){return getDir()+"/pages";}
    public final String getPagePath(Chapter chapter,int page){
        return chapter!=null ? getPagePath(chapter,chapter.getPage(page)) : null;
    }
    public final String getPagePath(Chapter chapter,Page page){
        return (chapter!=null && page!=null) ? getPagesPath()+"/"+chapter.getVol()+"--"+chapter.getNum()+"--"+page.getNum() : null;
    }
    public final File getPage(Chapter chapter,Page page){
        return (chapter!=null && page!=null) ? new File(getPagePath(chapter,page)) : null;
    }
    public Drawable getPage(Chapter chapter,int page){return Drawable.createFromPath(getPagePath(chapter,page));}
    public final Drawable loadThumbnail(){return loadThumbnail(getCoverPath());}
    public static Drawable loadThumbnail(String path){return Drawable.createFromPath(path);}
    public static void loadThumbnail(String path,String url,Callback<Drawable> callback){
        new Thread(() -> {
            Message message=new Message();
            message.obj=Drawable.createFromPath(path);
            if(message.obj==null){
                if(LoadTask.loadInBackground(url,null,new File(path),null,null,false)){
                    message.obj=Drawable.createFromPath(path);
                }
            }
            new Handler(Looper.getMainLooper()){@Override public void handleMessage(Message msg){callback.call((Drawable)msg.obj);}}.sendMessage(message);
        }).start();
    }
    public final void loadThumbnail(Callback<Drawable> callback){loadThumbnail(getCoverPath(),thumbnail,callback);}

    public final JSONObject toJSON(){
        try{
            return new JSONObject().put(FN[0],getProviderName())
                    .put(FN[1],url).put(FN[2],id).put(FN[3],name).put(FN[4], name_alt).put(FN[5],author).put(FN[6],author_url)
                    .put(FN[7],genres).put(FN[8],rating).put(FN[9],status).put(FN[10],description).put(FN[11],thumbnail).put(FN[12],url_web)
                    .put(FN[13],Chapter.toJSON(chapters)).put(FN[14],BookMark.toJSON(bookMarks)).put(FN[15],history!=null ? history.toJSON() : null)
                    .put(FN[16],CategoryFavorite).put(FN[17],lastSize).put(FN[18],dir).put(FN[19],lastTimeSave).put(FN[20],lastTimeFavorite).put(FN[21],lastMaxChapters);
        }catch(JSONException e){e.printStackTrace();}
    return null;}

    @Override
    public final String toString(){return Objects.requireNonNull(this.toJSON()).toString();}

    public static Manga fromJSON(String json){
        if(json!=null && json.length()>0){try{return fromJSON(new JSONObject(json));}catch(JSONException e){e.printStackTrace();}}
    return null;}
    public static Manga fromJSON(JSONObject json) throws JSONException{
        return newInstance(
                json.optString(FN[0],null),
                json.optString(FN[1],null),
                json.optInt(FN[2],-1),
                json.optString(FN[3],null),
                json.optString(FN[4],null),
                json.optString(FN[5],null),
                json.optString(FN[6],null),
                json.optString(FN[7],null),
                json.optDouble(FN[8],-1),
                json.optInt(FN[9],-1),
                json.optString(FN[10],null),
                json.optString(FN[11],null),
                json.optString(FN[12],null),
                Chapter.fromJSON(json.optJSONArray(FN[13])),
                BookMark.fromJSON(json.optJSONArray(FN[14])),
                BookMark.fromJSON(json.optJSONObject(FN[15])),
                json.optString(FN[16],null),
                json.optInt(FN[17],-1),
                json.optString(FN[18],null),
                json.optLong(FN[19],0),
                json.optLong(FN[20],0),
                json.optInt(FN[21],0)
        );
    }

    public static Manga loadFromStorage(String filePath){
        return loadFromStorage(new File(filePath));
    }
    public static Manga loadFromStorage(File file){
        BufferedReader in=null; Manga manga=null;
        if(file.exists()){
            try{
                in=new BufferedReader(new FileReader(file));
                StringBuilder b=new StringBuilder();
                String line;
                while((line=in.readLine())!=null){b.append(line).append("\n");}
                manga=Manga.fromJSON(b.toString());
                if(manga!=null){manga.dir=file.getParent();}
            }catch(IOException e){
                Logs.saveLog(e);
            }finally{
                try{if(in!=null){in.close();}}catch(IOException e){e.printStackTrace();}
            }
        }
        return manga;
    }

    public final void save(){
        Manga manga=this.clone();
        manga.chapters.clear();
        for(Chapter chapter : this.chapters){if(checkChapter(chapter)){manga.chapters.add(chapter);}}
        FileOutputStream out=null;
        try{
            out=new FileOutputStream(getInfoPath());
            out.write(manga.toString().getBytes());
        }catch(IOException e){
            e.printStackTrace();
        }finally{
            try{if(out!=null){out.close();}}catch(IOException e){e.printStackTrace();}
        }
    }

    public final void moveTo(String path){
        if(dir==null){
            setDir(path); save();
        } else if(Utils.File.move(new File(dir),new File(setDir(path)))){
            save();
        }
    }
    public final void delete(){Utils.File.delete(new File(dir));}
    public final void deleteAllPages(){Utils.File.delete(new File(getPagesPath()));}

    public final boolean checkChapterInfo(int chapter){return (chapter>=0 && chapter<chapters.size() && checkChapterInfo(chapters.get(chapter)));}
    public static boolean checkChapterInfo(Chapter chapter){return chapter.getPages()!=null && chapter.getPages().size()>0;}

    public final boolean checkChapter(Chapter chapter){
        if(checkChapterInfo(chapter)){
            for(Page page : chapter.getPages()){
                if(getPage(chapter, page).exists()){
                    return true;
                }
            }
        }
    return false;}

    public int countSaved(){int i=0; for(Chapter chapter:chapters){if(checkChapter(chapter)){i++;}} return i;}

    public static int getNumChapter(List<Chapter> list,Chapter chapter){if(chapter!=null && list!=null){for(int i=0;i<list.size();i++){if(chapter.equals(list.get(i))){return i;}}}return -1;}
    public final int getNumChapter(Chapter chapter){return getNumChapter(chapters,chapter);}
    public final int getNumChapter(BookMark bookMark){return getNumChapter(bookMark!=null ? bookMark.getChapter() : null);}
    public final int getNumChapterHistory(){return getNumChapter(history);}

    public final boolean updateDetails(){return updateDetails(loadFromStorage(getInfoPath()));}
    public final boolean updateDetails(Manga manga){
        if(manga!=null && this.hashCode()==manga.hashCode()){
            updateChapters(manga.chapters);
            updateBookMarks(manga.bookMarks);
            this.history=manga.history;
            this.lastSize=manga.lastSize;
            this.lastTimeSave=manga.lastTimeSave;
            this.CategoryFavorite=manga.CategoryFavorite;
            this.lastTimeFavorite=manga.lastTimeFavorite;
            return true;
        }
        return false;
    }

    public final void updateChapters(List<Chapter> chapters){
        boolean b=chapters.size()>this.chapters.size();
        for(Chapter saved: b?this.chapters:chapters){
            for(Chapter source: b?chapters:this.chapters){
                if(saved.equals(source) && source.countPages()==0){
                    source.pages=saved.getPages(); break;
                }
            }
        }
        if(b){
            this.chapters.clear();
            this.chapters.addAll(chapters);
        }
    }
    public final void updateBookMarks(ArrayList<BookMark> bookMarks){
        bookMarks.removeAll(Collections.singleton(null));
        this.bookMarks.clear();
        this.bookMarks.addAll(bookMarks);
        for(Chapter chapter:chapters){
            for(BookMark bookMark:this.bookMarks){
                if(chapter.equals(bookMark.getChapter())){
                    bookMark.chapter=chapter;
                }
            }
        }
    }
    public final void addBookMark(Chapter chapter,int page){
        if(chapter!=null){
            bookMarks.add(new BookMark(chapter,page));
            save();
        }
    }

    public final void removeBookMark(BookMark bookMark){
        if(bookMarks!=null){
            bookMarks.remove(bookMark);
            save();
        }
    }

    public final void clearHistory(){
        history=null;
        lastSize=0;
        save();
    }
    public final void createHistory(Chapter chapter,int page){
        if(chapter!=null){
            history=new BookMark(chapter,page);
            save();
        }
    }

    public final void clearChapters(int start,int end){
        for(int i=start;i<end;i++){
            Chapter chapter=chapters.get(i);
            if(chapter!=null && chapter.getPages()!=null){
                for(Page page: chapter.getPages()){getPage(chapter,page).delete();}
            }
        }
    }

    public final void loadChapters(final Context context, LoadService.Task task){
        context.startService(task.toIntent(new Intent(context, LoadService.class).setAction(LoadService.actionLoad)));
    }

    public void update(Context context,final Runnable onUpdated,final Callback<Throwable> onNotUpdated){
        if(NetworkUtils.isNetworkAvailable(context)){
            new Thread(()->{
                Message msg=new Message();
                try{
                    if(!updated){updated=update();}
                }catch(Throwable e){updated=false; Logs.saveLog(e); msg.obj=e;}
                new Handler(Looper.getMainLooper()){
                    @Override
                    public void handleMessage(Message msg){
                        if(updated){
                            if(onUpdated!=null){onUpdated.run();}
                        }else{
                            if(onNotUpdated!=null){onNotUpdated.call((Throwable) msg.obj);}
                        }
                    }
                }.sendMessage(msg);
            }).start();
        }else{if(onNotUpdated!=null){onNotUpdated.call(null);}}
    }
    public boolean isUpdated(){return updated;}
    public int getCheckedNew(){int size=chapters!=null ? chapters.size() : 0; return size>lastSize ? size-lastSize : 0;}
    public void seen(int chapter){
        lastSize=Math.max(chapter+1, lastSize);
        if(chapter+1>lastMaxChapters){lastMaxChapters=chapter+1;}
    }
    public boolean isNew(Chapter chapter){return lastSize<=chapters.indexOf(chapter);}
    public void checkedNew(){lastMaxChapters=chapters.size(); save();}
    public int getNotCheckedNew(){return Math.max(chapters.size()-lastMaxChapters,0);}

    public long getHistoryDate(){return history!=null ? history.getDate() : 0;}
    public final void setLastTimeSave(){this.lastTimeSave=System.currentTimeMillis();}
    public final long getLastTimeSave(){return this.lastTimeSave;}
    public final void setCategoryFavorite(String category){
        CategoryFavorite=category;
        if(category!=null){setLastTimeFavorite();}
        else{this.lastTimeFavorite=0;}
        save();
    }
    public final String getCategoryFavorite(){
        return this.CategoryFavorite;
    }
    public final void setLastTimeFavorite(){this.lastTimeFavorite=System.currentTimeMillis();}
    public final long getLastTimeFavorite(){return this.lastTimeFavorite;}
    private long ImagesSize=-1;
    public long recalculateImagesSize(){return ImagesSize=Utils.File.getSize(new File(getPagesPath()));}
    public long getImagesSize(){return ImagesSize<0 ? recalculateImagesSize() : ImagesSize;}

    public static CharSequence getSourceDescription(String source){
        return Manga_Scripted.getSourceDescription(source);
    }
    public static FilterSortAdapter getFilterSortAdapter(String source){
        return Manga_Scripted.createAdvancedSearchAdapter(source);
    }

    public CharSequence getGenres(ClickSpan.SpanClickListener listener){
        FilterSortAdapter adapter=Manga.getFilterSortAdapter(getProviderName());
        return adapter!=null && genres!=null ? adapter.getClickableSpans(genres, listener) : genres;
    }

    public static final Comparator<Manga> HistoryComparator=(o1,o2)->Long.compare(o2.getHistoryDate(),o1.getHistoryDate());
    public static final Comparator<Manga> SavingTimeComparator=(o1,o2)->Long.compare(o2.lastTimeSave,o1.lastTimeSave);
    public static final Comparator<Manga> FavoriteTimeComparator=(o1,o2)->Long.compare(o2.lastTimeFavorite,o1.lastTimeFavorite);
    public static final Comparator<Manga> AlphabeticalComparatorEn=(o1,o2)->String.CASE_INSENSITIVE_ORDER.compare(Objects.toString(o1.getAnyName(true),""),Objects.toString(o2.getAnyName(true),""));
    public static final Comparator<Manga> AlphabeticalComparator=(o1, o2)->String.CASE_INSENSITIVE_ORDER.compare(Objects.toString(o1.getAnyName(false),""),Objects.toString(o2.getAnyName(false),""));
    public static final Comparator<Manga> ImagesSizesComparator=(o1,o2)->Long.compare(o2.getImagesSize(),o1.getImagesSize());
    public static final Comparator<Manga> RatingComparator=(o1,o2)->Double.compare(o1.rating,o2.rating);
    public static final Comparator<Manga> SourceComparator(List<String> sources){return Comparator.comparingInt(o -> sources.indexOf(o.getProviderName()));}
}
