package org.alex.kitsune.manga;

import android.text.Html;
import org.alex.json.JSON;
import org.alex.kitsune.scripts.Script;
import org.alex.kitsune.manga.search.FilterSortAdapter;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.utils.Utils;
import org.json.JSONException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Manga_Scripted extends Manga{
    private static final Hashtable<String, Script> scripts=new Hashtable<>();
    public static Hashtable<String,Script> getScripts(){return scripts;}
    public static void setScripts(Hashtable<String,Script> scripts){Manga_Scripted.scripts.clear(); Manga_Scripted.scripts.putAll(scripts);}
    public static Script getScript(String source){return source!=null ? scripts.get(source) : null;}
    public static Script getScript(String source, Script def){return source!=null ? scripts.getOrDefault(source, def) : def;}
    public static Script getScript(String source, String path) throws Throwable {
        return getScript(source, Script.getInstance(path));
    }
    private final Script script;
    public static Manga_Scripted newInstance(Map<String,?> map){
        return newInstance(new JSON.Object(map));
    }
    public static Manga_Scripted newInstance(JSON.Object json){
        return newInstance(getScript(json.get(Constants.source,json.getString("Source"))),json);
    }
    public static Manga_Scripted newInstance(Script script,Map<String,?> json){
        return newInstance(script,new JSON.Object(json));
    }
    public static Manga_Scripted newInstance(Script script,JSON.Object json){
        return script!=null? new Manga_Scripted(script,json):null;
    }
    public Manga_Scripted(Script script, JSON.Object json) {
        super(json);
        this.script=script;
        json.put(Constants.domain,script.getString(Constants.domain,null));
        json.put(Constants.source,script.getString(Constants.source,null));
    }
    @Override
    public String getDomain(){return getString(Constants.domain);}

    @Override
    public String getSource(){return getString(Constants.source);}

    public static String getSourceDescription(String source){return getSourceDescription(getScript(source));}
    public static String getSourceDescription(Script script){return script.getString(Constants.description,null);}

    public static String fromHtml(String string){
        return string!=null?
                string.startsWith("http://") || string.startsWith("https://") ?
                        string
                        :
                        Utils.unescape_unicodes(Html.fromHtml(string,Html.FROM_HTML_MODE_LEGACY).toString())
                :
                null;
    }
    @Override
    public boolean update() throws Exception {
        Map<String,Object> map=script.invokeMethod(Constants.methodUpdate,Map.class,getUrl());
        if(map!=null){
            if(map.get("url_web")==null){
                map.put("url_web",map.get("url"));
            }
            List<Chapter> chapters=map.remove("chapters") instanceof List<?> list ? JSON.filter(list,Chapter.class) : null;
            List<?> similar=map.remove("similar") instanceof List<?> list ? list : null;
            if(get("edited",false)){
                map.remove("name");
                map.remove("name_alt");
            }
            map.forEach((key,value)-> set(key,value instanceof String v ? fromHtml(v):value));
            if(chapters!=null){
                updateChapters(chapters);
            }
            if(similar!=null){
                updateSimilar(getSimilar(similar.stream().map(e->e instanceof Map<?,?> m? (Map<String,?>)m:null).collect(Collectors.toList())));
            }
            return true;
        }
        return false;
    }

    @Override
    public List<Page> getPages(Chapter chapter) throws IOException, JSONException {
        return chapter.setPages((List<Page>)script.invokeMethod(Constants.methodGetPages,List.class,getUrl(),chapter.toJSON()));
    }
    public static List<Manga> query(String source,String name,int page,Object... params){return query(getScript(source),name,page,params);}
    public static List<Manga> query(Script script,String name,int page,Object... params){
        List<Map<String,?>> list=script.invokeMethod(Constants.methodQuery,List.class,name,page,params);
        return list.stream().map(m->newInstance(script,m)).filter(Objects::nonNull).collect(Collectors.toList());
    }
    public static List<Manga> query(String source,String url,int page){return query(getScript(source),url,page);}
    public static List<Manga> query(Script script,String url,int page){
        List<Map<String,?>> list=script.invokeMethod(Constants.methodQueryURL,List.class,url,page);
        return list.stream().map(m->newInstance(script,m)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public Set<Manga> loadSimilar() throws Exception {
        return getSimilar((List<Map<String,?>>)script.invokeMethod(Constants.methodLoadSimilar,List.class,info));
    }
    private Set<Manga> getSimilar(List<Map<String,?>> similar){
        return similar.stream().filter(Objects::nonNull).map(m->newInstance(script,m)).collect(Collectors.toSet());
    }

    public static FilterSortAdapter createAdvancedSearchAdapter(String source){return createAdvancedSearchAdapter(getScript(source));}
    public static FilterSortAdapter createAdvancedSearchAdapter(Script script){
        try{return script!=null ? new FilterSortAdapter(script, script.invokeMethod(Constants.methodCreateAdvancedSearchOptions,List.class)) : null;}catch(Exception e){e.printStackTrace(); return null;}
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
            String provider=entry.getValue().getString(Constants.domain,null);
            if(provider!=null && url.contains(provider)){
                return newInstance(new JSON.Object().put(Constants.source,entry.getValue()).put("url",url));
            }
        }
        return null;
    }
}
