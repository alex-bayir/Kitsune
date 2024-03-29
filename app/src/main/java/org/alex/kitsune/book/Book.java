package org.alex.kitsune.book;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import androidx.annotation.Nullable;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.Callback2;
import org.alex.kitsune.commons.ClickSpan;
import com.alex.json.java.JSON;
import org.alex.kitsune.commons.ListSet;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.book.search.FilterSortAdapter;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.services.LoadService;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class Book {
    protected JSON.Object info;

    protected final ListSet<Book> similar=new ListSet<>(new ArrayList<>(10));
    private boolean updated=false;
    public Book(JSON.Object json){
        this(
                json,
                Chapter.fromJSON(JSON.filter(json.getArray("chapters"),JSON.Object.class)),
                BookMark.fromJSON(JSON.filter(json.getArray("bookmarks"),JSON.Object.class)),
                BookMark.fromJSON(json.getObject("history"))
        );
    }
    public Book(JSON.Object info, List<Chapter> chapters, List<BookMark> bookMarks, BookMark history){
        this.info=info; if(info==null){throw new IllegalArgumentException("Json info cannot be null");}
        if(getUrl_WEB()==null){set("url_web",getUrl());}
        this.info.put("chapters",chapters!=null ? chapters : new ArrayList<>());
        this.info.put("bookmarks",bookMarks!=null ? bookMarks : new ArrayList<>());
        this.info.put("history",history);
        set("lastSize",get("lastSize",getChapters().size()));
        this.info.putIfAbsent("Id",getUrl().hashCode());
    }

    public static Book newInstance(JSON.Object json){
        return Book_Scripted.newInstance(json);
    }
    public static Book newInstance(Map<String,?> map){
        return newInstance(new JSON.Object(map));
    }

    public abstract String getDomain();
    public abstract String getSource();
    public abstract String getType();
    public final String getStatus(){return getStatus(null);}
    public final String getStatus(Context context){return getStatus(getString("status"),context);}
    private static String getStatus(String status,Context context){
        return switch (status!=null ? status : "None") {
            case "Announce" -> context==null? "Announce" : context.getString(R.string.Announce);
            case "Ongoing" ->  context==null? "Ongoing" : context.getString(R.string.Ongoing);
            case "Paused" ->  context==null? "Paused" : context.getString(R.string.Paused);
            case "Stopped" ->  context==null? "Stopped" : context.getString(R.string.Stopped);
            case "Finished" ->  context==null? "Finished" : context.getString(R.string.Finished);
            default ->  context==null? "None" : context.getString(R.string.None);
        };
    }
    public abstract boolean update() throws Exception;

    public final List<Page> getPages(int chapter) throws IOException{
        return getPages(getChapters().get(chapter));
    }
    public abstract List<Page> getPages(Chapter chapter) throws IOException;
    protected final void updateSimilar(Set<Book> books){
        books.removeAll(Collections.singleton(null));
        if(books.size()!=0){
            similar.clear();
            similar.addAll(books);
        }
    }
    protected abstract Set<Book> loadSimilar() throws Exception;
    public final void loadSimilar(Callback<Set<Book>> callback, Callback<Throwable> error){
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

    public final int getId(){return get("Id",getUrl().hashCode());}
    public final String getUrl(){return getString("url");}
    public String getUrl_WEB(){return getString("url_web");}
    public final String getName(){return getString("name");}
    public final String getNameAlt(){return getString("name_alt");}
    public final String getAnyName(){return getAnyName(true);}
    public final String getAnyName(boolean alt){return getAnyName(alt,null);}
    public final String getAnyName(boolean alt,String def){return alt ? (getNameAlt()!=null ? getNameAlt() : (getName()!=null? getName() : def)) : (getName()!=null ? getName() : (getNameAlt()!=null ? getNameAlt() : def));}
    public final Object getAuthors(){return get("authors");}
    public final Object getArtists(){return get("artists");}
    public final Object getPublishers(){return get("publishers");}
    public final String getGenres(){return getString("genres");}
    public final String getTags(){return getString("tags");}
    public final String getCover(){return get("cover",getString("thumbnail"));}
    public final double getRating(){return get("rating",0.0);}
    public final String getDescription(){return getString("description");}
    public final CharSequence getDescription(int flags){return getDescription(flags,null);}
    public final CharSequence getDescription(int flags, String def){return getDescription()!=null && getDescription().length()>0 ? Html.fromHtml(getDescription(), flags) : def;}
    public final BookMark getHistory(){return (BookMark)get("history");}
    public final List<Chapter> getChapters(){return (List<Chapter>)get("chapters");}
    public final List<BookMark> getBookMarks(){return (List<BookMark>)get("bookmarks");}
    public final ListSet<Book> getSimilar(){return similar;}
    public final int getLastSize(){return get("lastSize",0);}
    @Override public final int hashCode(){return getId();}
    @Override public final boolean equals(@Nullable Object obj){return obj instanceof Book && hashCode()==obj.hashCode();}

    public final String setDir(String dir){set("dir",dir+(dir.endsWith(File.separator) ? "" :File.separator)+hashCode()); return getDir();}
    public final String getDir(){return getString("dir");}
    public final String getCoverPath(){return getDir()+File.separator+"card";}
    public final String getInfoPath(){return getDir()+File.separator+"summary";}
    public final String getPagesPath(){return getDir()+File.separator+"pages";}
    public final String getPagePath(Chapter chapter,int page){
        return chapter!=null ? getPagePath(chapter,chapter.getPage(page)) : null;
    }
    public final String getPagePath(Chapter chapter,Page page){
        return (chapter!=null && page!=null) ? getPagesPath()+File.separator+chapter.getVol()+"--"+chapter.getNum()+"--"+page.getNum() : null;
    }
    public final File getPage(Chapter chapter,Page page){
        return (chapter!=null && page!=null) ? new File(getPagePath(chapter,page)) : null;
    }
    public Drawable getPage(Chapter chapter,int page){return Drawable.createFromPath(getPagePath(chapter,page));}
    public boolean load(Chapter chapter,Page page, Boolean cancel_flag){
        return load(chapter,page,page.getData(),cancel_flag,null)==null;
    }
    public Throwable load(Chapter chapter,Page page,String data, Boolean cancel_flag, Callback2<Long,Long> process){
        return load(chapter,page,data,Utils.isUrl(data),cancel_flag,process);
    }
    public Throwable load(Chapter chapter,Page page,String data,boolean url, Boolean cancel_flag, Callback2<Long,Long> process){
        return load(getPage(chapter,page),data,url,cancel_flag,process);
    }
    protected abstract Throwable load(File save,String data,boolean url, Boolean cancel_flag, Callback2<Long,Long> process);
    public final Drawable loadCover(){return loadCover(getCoverPath());}
    public static Drawable loadCover(String path){return Drawable.createFromPath(path);}
    public static void loadCover(String path, String url, String domain, Callback<Drawable> callback){
        new Thread(() -> {
            Drawable loaded=Drawable.createFromPath(path);
            if(loaded==null && NetworkUtils.load(url,domain,new File(path))){
                loaded=Drawable.createFromPath(path);
            }
            Drawable drawable=loaded;
            NetworkUtils.getMainHandler().post(()->callback.call(drawable));
        }).start();
    }
    public final void loadCover(Callback<Drawable> callback){loadCover(getCoverPath(),getCover(),get("domain",null),callback);}

    private List<Chapter> filter(boolean full, List<Chapter> chapters){
        return full ? chapters : chapters.stream().filter(this::checkChapter).collect(Collectors.toList());
    }
    public final JSON.Object toJSON(){
        return new JSON.Object(info).put("chapters",Chapter.toJSON(getChapters()))
                .put("bookmarks",BookMark.toJSON(getBookMarks()))
                .put("history",getHistory()!=null ? getHistory().toJSON() : null);
    }
    public final JSON.Object toJSON(boolean full){
        return new JSON.Object(info)
                .put("chapters",Chapter.toJSON(filter(full,getChapters())))
                .put("bookmarks",BookMark.toJSON(getBookMarks()))
                .put("history",getHistory()!=null ? getHistory().toJSON() : null);
    }

    @NotNull
    @Override
    public final String toString(){return this.toJSON().json(1);}

    public static Book fromJSON(String json){
        return json!=null && json.length()>0 ? fromJSON(new StringReader(json)):null;
    }
    public static Book fromJSON(Reader reader){
        Book book=null;
        try{
            book=fromJSON(JSON.Object.create(reader));
            reader.close();
        }catch(IOException e){
            e.printStackTrace();
        }
        return book;
    }
    public static Book fromJSON(JSON.Object json){
        return newInstance(json);
    }
    public static Book loadFromStorage(String filePath){
        return loadFromStorage(new File(filePath));
    }
    public static Book loadFromStorage(File file){
        Book book=null;
        if(file.exists()){
            try{
                book=Book.fromJSON(new FileReader(file));
                if(book!=null){book.set("dir",file.getParent());}
            }catch(IOException e){
                Logs.saveLog(e);
            }
        }
        return book;
    }

    public final void save(){
        try{
            new File(getDir()).mkdirs();
            toJSON(false).json(new File(getInfoPath()),1);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public final void moveTo(String path){
        if(getDir()==null){
            setDir(path); save();
        } else {
            Utils.File.move(new File(getDir()),new File(setDir(path)));
            save();
        }
    }
    public final void delete(){
        Utils.File.delete(new File(getDir()));
    }
    public final void deleteAllPages(){
        Utils.File.delete(new File(getPagesPath()));
    }

    public final boolean checkChapterInfo(int chapter,List<Chapter> chapters){return (chapter>=0 && chapter<chapters.size() && checkChapterInfo(chapters.get(chapter)));}
    public final boolean checkChapterInfo(int chapter){return checkChapterInfo(chapter,getChapters());}
    public static boolean checkChapterInfo(Chapter chapter){return chapter.getPages()!=null && chapter.getPages().size()>0;}

    public final boolean checkChapter(Chapter chapter){
        if(checkChapterInfo(chapter)){
            for(Page page : chapter.getPages()){
                if(getPage(chapter, page).exists()){
                    return true;
                }
            }
        }
        return false;
    }

    public int countSaved(){
        List<Chapter> chapters=getChapters(); int count=0;
        for(int i=0;i<chapters.size();i++){if(checkChapter(chapters.get(i))){count++;}}
        return count;
    }

    public static int getNumChapter(List<Chapter> list,Chapter chapter){if(chapter!=null && list!=null){for(int i=0;i<list.size();i++){if(chapter.equals(list.get(i))){return i;}}}return -1;}
    public final int getNumChapter(Chapter chapter){return getNumChapter(getChapters(),chapter);}
    public final int getNumChapter(BookMark bookMark){return getNumChapter(bookMark!=null ? bookMark.getChapter() : null);}
    public final int getNumChapterHistory(){return getNumChapter(getHistory());}

    public final void updateDetails(){updateDetails(loadFromStorage(getInfoPath()));}
    public final void updateDetails(Book book){
        if(book!=null && this.hashCode()==book.hashCode()){
            updateChapters(book.getChapters());
            updateBookMarks(book.getBookMarks());
            set("history", book.getHistory());
            set("lastSize", book.getLastSize());
            set("lastTimeSave", book.getLastTimeSave());
            set("category", book.getCategory());
            set("category time", book.getCategoryTime());
        }
    }

    public final void updateChapters(List<Chapter> chapters){
        boolean b=chapters.size()>=getChapters().size();
        for(Chapter saved: b?getChapters():chapters){
            for(Chapter source: b?chapters:getChapters()){
                if(saved.equals(source) && source.countPages()==0){
                    source.setPages(saved.getPages()); break;
                }
            }
        }
        if(b){
            getChapters().clear();
            getChapters().addAll(chapters);
        }
    }
    public final void updateBookMarks(List<BookMark> bookMarks){
        bookMarks.removeAll(Collections.singleton(null));
        getBookMarks().clear();
        getBookMarks().addAll(bookMarks);
        for(Chapter chapter:getChapters()){
            for(BookMark bookMark:getBookMarks()){
                if(chapter.equals(bookMark.getChapter())){
                    bookMark.chapter=chapter;
                }
            }
        }
    }
    public final void addBookMark(Chapter chapter,int page){
        if(chapter!=null){
            getBookMarks().add(new BookMark(chapter,page));
            save();
        }
    }

    public final void removeBookMark(BookMark bookMark){
        if(getBookMarks()!=null){
            getBookMarks().remove(bookMark);
            save();
        }
    }

    public final void clearHistory(){
        set("history",null);
        set("lastSize",0);
        save();
    }
    public final void createHistory(Chapter chapter,int page){
        if(chapter!=null){
            set("history",new BookMark(chapter,page));
            save();
        }
    }

    public final void clearChapter(int index){
        Chapter chapter=getChapters().get(index);
        if(chapter!=null && chapter.getPages()!=null){
            for(Page page: chapter.getPages()){getPage(chapter,page).delete();}
        }
    }

    public final void loadChapters(final Context context, LoadService.Task task){
        context.startService(task.toIntent(new Intent(context, LoadService.class).setAction(LoadService.actionLoad)));
    }

    public void update(Callback<Boolean> update,Callback<Throwable> error, boolean re_updating){
        new Thread(()->{
            if(!updated || re_updating){
                try{
                    updated=update();
                }catch(Throwable e){
                    updated=false;
                    Logs.saveLog(e);
                    if(error!=null){
                        new Handler(Looper.getMainLooper()).post(()->error.call(e));
                    }
                }
            }
            if(update!=null){
                new Handler(Looper.getMainLooper()).post(()->update.call(updated));
            }
        }).start();
    }
    public void update(Callback<Boolean> update,Callback<Throwable> error){
        update(update,error,false);
    }
    public void setNames(String name,String name_alt){set("name",name); set("name_alt",name_alt); setEdited(true);}
    public void setEdited(boolean edited){set("edited",edited); save();}
    public boolean isUpdated(){return updated;}
    public int getCheckedNew(){int size=getChapters()!=null ? getChapters().size() : 0; return size>getLastSize() ? size-getLastSize() : 0;}
    public void seen(int chapter){
        set("lastSize",Math.max(chapter+1, getLastSize()));
        if(chapter+1>get("lastMaxChapters",0)){set("lastMaxChapters",chapter+1);}
    }
    public boolean isNew(Chapter chapter){return getLastSize()<=getChapters().indexOf(chapter);}
    public void checkedNew(){set("lastMaxChapters",getChapters().size()); save();}
    public int getNotCheckedNew(){return Math.max(getChapters().size()-get("lastMaxChapters",0),0);}

    public long getHistoryDate(){return getHistory()!=null ? getHistory().getDate() : 0;}
    public final void setLastTimeSave(){set("lastTimeSave",System.currentTimeMillis());}
    public final long getLastTimeSave(){return get("lastTimeSave",0L);}
    public final void setCategory(String category){
        set("category",category); set("CategoryFavorite",null);
        set("category time",category==null?0L:System.currentTimeMillis());
        save();
    }
    public final String getCategory(){
        return get("category",getString("CategoryFavorite"));
    }
    public final long getCategoryTime(){return get("category time",get("lastTimeFavorite",0L));}
    private long ImagesSize=-1;
    public long calculateImagesSize(){return ImagesSize=Utils.File.getSize(new File(getPagesPath()));}
    public long getImagesSize(){return ImagesSize<0 ? calculateImagesSize() : ImagesSize;}

    public static CharSequence getSourceDescription(String source){
        return Book_Scripted.getSourceDescription(source);
    }
    public static FilterSortAdapter getFilterSortAdapter(String source){
        return Book_Scripted.createAdvancedSearchAdapter(source);
    }

    private CharSequence get(Function<Void,String> function,ClickSpan.SpanClickListener listener){
        FilterSortAdapter adapter=Book.getFilterSortAdapter(getSource());
        String text=function.apply(null);
        return adapter!=null && text!=null ? adapter.getClickableSpans(text, listener) : text;
    }
    public CharSequence getGenres(ClickSpan.SpanClickListener listener){
        return get(v->getGenres(),listener);
    }
    public CharSequence getTags(ClickSpan.SpanClickListener listener){
        return get(v->getTags(),listener);
    }

    public static final Comparator<Book> HistoryComparator=Comparator.comparingLong(Book::getHistoryDate).reversed();
    public static final Comparator<Book> SavingTimeComparator=Comparator.comparingLong(Book::getLastTimeSave).reversed();
    public static final Comparator<Book> CategoryTimeComparator=Comparator.comparingLong(Book::getCategoryTime).reversed();
    public static final Comparator<Book> AlphabeticalComparatorAlt=Comparator.comparing(book -> book.getAnyName(true,""),String.CASE_INSENSITIVE_ORDER);
    public static final Comparator<Book> AlphabeticalComparator=Comparator.comparing(book -> book.getAnyName(false,""),String.CASE_INSENSITIVE_ORDER);
    public static final Comparator<Book> ImagesSizesComparator=Comparator.comparingLong(Book::getImagesSize).reversed();
    public static final Comparator<Book> RatingComparator=Comparator.comparingDouble(Book::getRating).reversed();

    public final void set(String key,Object value){
        info.put(key,value);
    }
    public final Object get(String key){
        return info.get(key);
    }
    public final Object get(String key,Object def){
        return info.get(key,def);
    }
    public final String get(String key,String def){
        return info.get(key,def);
    }
    public final String getString(String key){
        return info.getString(key);
    }
    public final int get(String key,int def){
        return info.get(key,def);
    }
    public final long get(String key,long def){
        return info.get(key,def);
    }
    public final double get(String key,double def){
        return info.get(key,def);
    }
    public final float get(String key,float def){
        return info.get(key,def);
    }
    public final boolean get(String key,boolean def){
        return info.get(key,def);
    }
    public final JSON.Object getObject(String key){
        return info.getObject(key);
    }
    public final JSON.Array<?> getArray(String key){
        return info.getArray(key);
    }
}
