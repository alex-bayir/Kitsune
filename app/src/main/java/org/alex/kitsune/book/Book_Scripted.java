package org.alex.kitsune.book;

import android.text.Html;
import com.alex.json.java.JSON;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.commons.Callback2;
import org.alex.kitsune.scripts.Script;
import org.alex.kitsune.book.search.FilterSortAdapter;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Book_Scripted extends Book {
    private static final Hashtable<String, Script> scripts=new Hashtable<>();
    public static Hashtable<String,Script> getScripts(){return scripts;}
    public static void setScripts(Hashtable<String,Script> scripts){
        Book_Scripted.scripts.clear(); Book_Scripted.scripts.putAll(scripts);}
    public static Script getScript(String source){return source!=null ? scripts.get(source) : null;}
    public static Script getScript(String source, Script def){return source!=null ? scripts.getOrDefault(source, def) : def;}
    public static Script getScript(String source, String path) throws Throwable {
        return getScript(source, Script.getInstance(path));
    }
    private final Script script;
    public static Book_Scripted newInstance(Map<String,?> map){
        return newInstance(new JSON.Object(map));
    }
    public static Book_Scripted newInstance(JSON.Object json){
        return newInstance(getScript(json.get(Constants.source,json.getString("Source"))),json);
    }
    public static Book_Scripted newInstance(Script script, Map<String,?> map){
        return newInstance(script,map instanceof JSON.Object json ? json : new JSON.Object(map));
    }
    public static Book_Scripted newInstance(Script script, JSON.Object json){
        return script!=null? new Book_Scripted(script,json):null;
    }
    public Book_Scripted(Script script, JSON.Object json) {
        super(json);
        this.script=script;
        json.put(Constants.domain,script.getString(Constants.domain,null));
        json.put(Constants.source,script.getString(Constants.source,null));
        json.put(Constants.Type,script.getString(Constants.Type,null));
    }
    @Override
    public String getDomain(){return getString(Constants.domain);}

    @Override
    public String getSource(){return getString(Constants.source);}

    @Override
    public String getType(){return getString(Constants.Type);}

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
    public List<Page> getPages(Chapter chapter) throws IOException {
        return chapter.setPages((List<Page>)script.invokeMethod(Constants.methodGetPages,List.class,getUrl(),chapter.toJSON()));
    }
    public static List<Book> query(String source, String name, int page, Object... params){return query(getScript(source),name,page,params);}
    public static List<Book> query(Script script, String name, int page, Object... params){
        List<Map<String,?>> list=script.invokeMethod(Constants.methodQuery,List.class,name,page,params);
        return list.stream().map(m->newInstance(script,m)).filter(Objects::nonNull).collect(Collectors.toList());
    }
    public static List<Book> query(String source, String url, int page){return query(getScript(source),url,page);}
    public static List<Book> query(Script script, String url, int page){
        List<Map<String,?>> list=script.invokeMethod(Constants.methodQueryURL,List.class,url,page);
        return list.stream().map(m->newInstance(script,m)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public Set<Book> loadSimilar() throws Exception {
        return getSimilar((List<Map<String,?>>)script.invokeMethod(Constants.methodLoadSimilar,List.class,info));
    }
    private Set<Book> getSimilar(List<Map<String,?>> similar){
        return similar.stream().filter(Objects::nonNull).map(Book_Scripted::determinate).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @Override
    public boolean loadPage(Chapter chapter, Page page, Callback<File> done, Boolean cancel_flag, Callback2<Long, Long> process, Callback<Throwable> onBreak) {
        if(page==null){return false;}
        File save=getPage(chapter, page);
        if(page.getUrl()!=null){
            if(NetworkUtils.load(NetworkUtils.getClient(script.getBoolean("descramble",false)),page.getUrl(),getDomain(),save,cancel_flag,process,onBreak,false)){
                done.call(save);
                return true;
            }
        }else if(page.getText()!=null){
            try{
                Utils.File.writeFile(save,page.getText(),false);
                done.call(save);
                return true;
            }catch (IOException e){
                if(onBreak!=null){onBreak.call(e);}
            }
        }else{
            try{
                int index=chapter.getPages().indexOf(page);
                getPages(chapter);
                return loadPage(chapter,chapter.getPage(index),done,cancel_flag,process,onBreak);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        return false;
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

    public static Script determinate_script(String url){
        if(url==null){return null;}
        int min=Integer.MAX_VALUE; Script script=null;
        String url_domain=Utils.group(url,"://(.*?)/");
        for(Map.Entry<String,Script> entry:scripts.entrySet()){
            String domain=entry.getValue().getString(Constants.domain,null);
            if(domain!=null){
                int distance=levenshtein_distance(url_domain,domain);
                if(distance<=min && distance<domain.length()/2){
                    min=distance; script=entry.getValue();
                }
            }
        }
        return script;
    }
    public static Book_Scripted determinate(String url){
        return determinate(new JSON.Object().put("url",url));
    }
    public static Book_Scripted determinate(Map<String,?> map){
        return newInstance(determinate_script(map.get("url") instanceof String str ? str : null),map);
    }

    private static int levenshtein_distance(CharSequence str1, CharSequence str2) {
        int[][] distance = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++){
            distance[i][0] = i;
        }
        for (int j = 1; j <= str2.length(); j++){
            distance[0][j] = j;
        }

        for (int i = 1; i <= str1.length(); i++)
            for (int j = 1; j <= str2.length(); j++)
                distance[i][j] = min(distance[i - 1][j] + 1,
                        distance[i][j - 1] + 1,
                        distance[i - 1][j - 1] + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1));

        return distance[str1.length()][str2.length()];
    }
    private static int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }
}
