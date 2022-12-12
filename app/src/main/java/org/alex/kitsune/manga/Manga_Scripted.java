package org.alex.kitsune.manga;

import org.alex.kitsune.scripts.Script;
import org.alex.kitsune.manga.search.FilterSortAdapter;
import org.alex.kitsune.ui.main.Constants;
import org.json.JSONException;
import java.io.IOException;
import java.util.*;

public class Manga_Scripted extends Manga{
    private static final Hashtable<String, Script> scripts=new Hashtable<>();
    public static Hashtable<String,Script> getScripts(){return scripts;}
    public static void setScripts(Hashtable<String,Script> scripts){Manga_Scripted.scripts.clear(); Manga_Scripted.scripts.putAll(scripts);}
    public static Script getScript(String source){return source!=null ? scripts.get(source) : null;}
    public static Script getScript(String source, Script def){return source!=null ? scripts.getOrDefault(source, def) : def;}
    public static Script getScript(String source, String path) throws Throwable {
        return getScript(source, Script.getInstance(path));
    }

    public String provider;
    public String providerName;
    private final Script script;

    public static Manga_Scripted newInstance(String providerName,String url,int id,String name,String name_alt,String author,String author_url,String genres,double rating,int status,String description,String thumbnail,String url_web,ArrayList<Chapter> chapters,ArrayList<BookMark> bookMarks,BookMark history,String CategoryFavorite,int lastSize,String dir,long lastTimeSave,long lastTimeFavorite,int lastMaxChapters,boolean edited){
        return newInstance(getScript(providerName),url,id,name,name_alt,author,author_url,genres,rating,status,description,thumbnail,url_web,chapters,bookMarks,history,CategoryFavorite,lastSize,dir,lastTimeSave,lastTimeFavorite,lastMaxChapters,edited);
    }
    public static Manga_Scripted newInstance(Script script,String url,int id,String name,String name_alt,String author,String author_url,String genres,double rating,int status,String description,String thumbnail,String url_web,ArrayList<Chapter> chapters,ArrayList<BookMark> bookMarks,BookMark history,String CategoryFavorite,int lastSize,String dir,long lastTimeSave,long lastTimeFavorite,int lastMaxChapters,boolean edited){
        return script==null ? null : new Manga_Scripted(script,url,id,name,name_alt,author,author_url,genres,rating,status,description,thumbnail,url_web,chapters,bookMarks,history,CategoryFavorite,lastSize,dir,lastTimeSave,lastTimeFavorite,lastMaxChapters,edited);
    }

    public Manga_Scripted(Script script,String url, int id, String name, String name_alt, String author, String author_url, String genres, double rating, int status, String description, String thumbnail, String url_web, ArrayList<Chapter> chapters, ArrayList<BookMark> bookMarks, BookMark history, String CategoryFavorite, int lastSize, String dir, long lastTimeSave, long lastTimeFavorite, int lastMaxChapters,boolean edited) {
        super(url, id, name, name_alt, author, author_url, genres, rating, status, description, thumbnail, url_web, chapters, bookMarks, history, CategoryFavorite, lastSize, dir, lastTimeSave, lastTimeFavorite, lastMaxChapters, edited);
        this.script=script;
        provider=script.getString(Constants.provider,null);
        providerName=script.getString(Constants.providerName,null);
    }

    @Override
    public String getProvider(){return provider;}

    @Override
    public String getProviderName(){return providerName;}

    public static String getSourceDescription(String source){return getSourceDescription(getScript(source));}
    public static String getSourceDescription(Script script){return script.getString(Constants.sourceDescription,null);}

    @Override
    public boolean update() throws Exception {
        Wrapper w=script.invokeMethod(Constants.methodUpdate,Wrapper.class,url);
        if(w!=null){
            id=w.id;
            if(!edited){
                name=w.name;
                name_alt=w.name_alt;
            }
            author=w.author;
            author_url=w.author_url;
            genres=w.genres;
            rating=w.rating;
            status=w.status;
            description=w.description;
            thumbnail=w.thumbnail;
            url_web=w.url_web;
            updateChapters(uniqueChapters(w.chapters));
            updateSimilar(getSimilar(w.similar));
            return true;
        }
        return false;
    }
    private static Wrapper wrap(Manga manga){
        return new Wrapper(manga.url,manga.id,manga.name,manga.name_alt,manga.author,manga.author_url,manga.genres,manga.rating,manga.status,manga.description,manga.thumbnail,manga.url_web,null,null);
    }
    private static Manga_Scripted unwrap(Script script,Wrapper w){
        return new Manga_Scripted(script, w.url, w.id, w.name, w.name_alt, w.author, w.author_url, w.genres, w.rating, w.status, w.description, w.thumbnail,w.url_web, null, null, null, null, 0, null, 0, 0, 0, false);
    }

    @Override
    public ArrayList<Page> getPages(Chapter chapter) throws IOException, JSONException {
        return chapter.pages=script.invokeMethod(Constants.methodGetPages,ArrayList.class,url,chapter);
    }

    public static ArrayList<Manga> query(String source,String name,int page,Object... params){return query(getScript(source),name,page,params);}
    public static ArrayList<Manga> query(Script script,String name,int page,Object... params){
        ArrayList<Wrapper> wrappers=new ArrayList<Wrapper>(script.invokeMethod(Constants.methodQuery,ArrayList.class,name,page,params));
        wrappers.removeAll(Collections.singleton(null));
        ArrayList<Manga> list=new ArrayList<>(wrappers.size());
        for(Wrapper w:wrappers){
            list.add(unwrap(script,w));
        }
        return list;
    }

    @Override
    public HashSet<Manga> loadSimilar() throws Exception {
        return getSimilar(new ArrayList<Wrapper>(script.invokeMethod(Constants.methodLoadSimilar,ArrayList.class,wrap(this))));
    }
    private HashSet<Manga> getSimilar(List<Wrapper> wrappers){
        wrappers.removeAll(Collections.singleton(null));
        HashSet<Manga> set=new HashSet<>(wrappers.size());
        for(Wrapper w:wrappers){
            set.add(unwrap(script,w));
        }
        return set;
    }

    public static FilterSortAdapter createAdvancedSearchAdapter(String source){return createAdvancedSearchAdapter(getScript(source));}
    public static FilterSortAdapter createAdvancedSearchAdapter(Script script){
        try{return script!=null ? new FilterSortAdapter(script, script.invokeMethod(Constants.methodCreateAdvancedSearchOptions,ArrayList.class)) : null;}catch(Exception e){e.printStackTrace(); return null;}
    }
    public static Set<String> getAllGenres(){
        HashSet<String> set=new HashSet<>();
        for(Script script:scripts.values()){
            FilterSortAdapter.getTitles(createAdvancedSearchAdapter(script), set);
        }
        return set;
    }

    public static Manga_Scripted determinate(String url){
        for(Map.Entry<String,Script> entry:scripts.entrySet()){
            String provider=entry.getValue().getString(Constants.provider,null);
            if(provider!=null && url.contains(provider)){
                return newInstance(entry.getValue(), url, 0, null, null, null, null, null, 0, 0, null, null, null, null, null, null, null, 0, null, 0, 0, 0,false);
            }
        }
        return null;
    }

    public static List<Chapter> uniqueChapters(List<Chapter> chapters){
        String translator=chapters.size()>0?chapters.get(0).getInfo():null;
        Set<String> translators=new LinkedHashSet<>();
        chapters.forEach(chapter -> translators.add(chapter.getInfo()));
        if(translators.size()>1){
            final HashMap<String,Chapter> map=new LinkedHashMap<>();
            chapters.forEach(chapter -> {
                String key=chapter.getVol()+"--"+chapter.getNum();
                if(Objects.equals(translator,chapter.getInfo())){
                    map.put(key,chapter);
                }else{
                    map.putIfAbsent(key,chapter);
                }
            });
            chapters.clear();
            chapters.addAll(map.values());
        }
        return chapters;
    }
}
