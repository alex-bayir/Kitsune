package org.alex.kitsune.scripts;

import com.blacksquircle.ui.language.base.Language;
import com.blacksquircle.ui.language.lua.LuaLanguage;
import okhttp3.*;
import org.alex.kitsune.R;
import com.alex.json.java.JSON;
import java.io.*;
import java.net.SocketTimeoutException;
import javax.net.ssl.SSLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.alex.kitsune.commons.Callback2;
import org.alex.kitsune.commons.HttpStatusException;
import org.alex.kitsune.commons.URLBuilder;
import org.alex.kitsune.book.Chapter;
import org.alex.kitsune.book.Page;
import org.alex.kitsune.book.search.Options;
import org.alex.kitsune.utils.NetworkUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.*;
import static org.alex.kitsune.utils.NetworkUtils.*;

public class Lua extends Script{
    public static final String extension="lua";
    private final Globals globals;
    public Lua(File script) throws Throwable {
        super(script, "Lua");
        globals=JsePlatform.standardGlobals();
        globals.load(new LuaJavaLib());
        globals.load("package.path=package.path..';"+(script.getParent()+File.separator).replace(File.separator,File.separator+File.separator)+"?.lua'").call();
        globals.load("function num(n) return n==nil and 0 or tonumber(n:match(\"[0-9]*%.?[0-9]+\")) end").call(); // init num(string) - safe tonumber function
        globals.set("network", Coercion.coerce(Network.class));
        globals.set("utils", Coercion.coerce(Utils.class));
        globals.set("JSONObject",Coercion.coerce(JSON.Object.class));
        globals.set("JSONArray",Coercion.coerce(JSON.Array.class));
        globals.set("Chapter",Coercion.coerce(Chapter.class));
        globals.set("Page",Coercion.coerce(Page.class));
        globals.set("Options",Coercion.coerce(Options.class));
        globals.loadfile(script.getAbsolutePath()).call();
        //globals.STDOUT=System.out;
        //globals.STDERR=System.err;
    }

    public Globals getGlobals(){
        return globals;
    }

    @Override
    public void setSTDOUT(PrintStream out){
        globals.STDOUT=out;
    }

    @Override
    public void setSTDERR(PrintStream err){
        globals.STDERR=err;
    }

    @Override
    public int getLanguageIconId(){
        return R.drawable.ic_lua;
    }

    @Override
    public void put(String name, Object object) {
        globals.set(name, CoerceJavaToLua.coerce(object));
    }

    @Override
    public <T> T get(String name,Class<T> return_type) {
        try{
            return Coercion.coerce(globals.get(name), return_type);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public <T> T invokeMethod(Object object, String name, Class<T> return_type, Object... params) {
        LuaValue[] values=new LuaValue[params.length];
        for(int i=0;i<values.length;i++){values[i]=Coercion.coerce(params[i]);}
        Varargs result=globals.get(name).invoke(values);
        return Coercion.coerce(result.arg1(),return_type);
    }
    public static class Coercion {
        private static <T> T coerce(LuaValue value,Class<T> return_type){
            if(value==null || value==LuaValue.NIL){return null;}
            if(return_type!=null){
                if(Void.class.isAssignableFrom(return_type)){
                    return null;
                }else if(Map.class.isAssignableFrom(return_type)){
                    return (T)coerce(value,new TreeMap<>(),true);
                }else if (List.class.isAssignableFrom(return_type)){
                    return (T)coerce(value,new LinkedList<>(),true);
                }else if (Set.class.isAssignableFrom(return_type)){
                    return (T)coerce(value,new LinkedHashSet<>(),true);
                }else {
                    return (T)CoerceLuaToJava.coerce(value, return_type);
                }
            }else{
                String clz=value.getClass().getSimpleName();
                return (T)switch (clz){
                    case "LuaNil","None"->null;
                    case "LuaBoolean"->coerce(value,Boolean.class);
                    case "LuaInteger"->coerce(value,Integer.class);
                    case "LuaDouble"->value.islong()?coerce(value,Long.class):coerce(value,Double.class);
                    case "LuaString"->coerce(value,String.class);
                    case "LuaTable"->coerce((LuaTable) value);
                    case "LuaUserData","JavaInstance"->value.optuserdata(null);
                    case "JavaArray"->coerce(value, Object[].class);
                    case "JavaClass"->coerce(value,Class.class);
                    default -> throw new IllegalArgumentException("Cannot coerce type "+clz);
                };
            }
        }
        private static Object coerce(LuaTable table){
            Map<?,?> map=coerce(table,new TreeMap<>());
            // This variant of check is available only for sorted values (TreeMap)
            boolean isList=true; Integer last=null;
            for(Object k:map.keySet()){
                if(k instanceof Integer num){
                    if(last!=null && num-last!=1){
                        isList=false; break;
                    }else{
                        last=num;
                    }
                }else{
                    isList=false; break;
                }
            }
            return isList ? new ArrayList<>(map.values()) : map;
        }
        private static Map<?,?> coerce(LuaValue value, Map<Object, Object> map,boolean def_is_null){
            return value instanceof LuaTable table ? coerce(table,map) : value instanceof LuaUserdata lud ? coerce(lud,map,def_is_null) : def_is_null ? null : map;
        }
        private static Map<?,?> coerce(LuaTable table, Map<Object, Object> map){
            for(LuaValue key:table.keys()){
                Object k=coerce(key, (Class<?>) null);
                Object v=coerce(table.get(key), (Class<?>) null);
                map.put(k,v);
            }
            return map;
        }
        private static Map<?,?> coerce(LuaUserdata table, Map<Object,Object> map, boolean def_is_null){
            if(table.m_instance instanceof Map m){
                if(map==null){
                    map=m;
                }else{
                    map.putAll(m);
                }
                return map;
            }else{
                return def_is_null ? null : map;
            }
        }
        private static Collection<?> coerce(LuaValue value, Collection<Object> collection, boolean def_is_null){
            if(value instanceof LuaTable table){
                collection.addAll(coerce(table,new TreeMap<>()).values());
                return collection;
            }else{
                return def_is_null ? null : collection;
            }
        }
        private static List<?> coerce(LuaValue value, List<Object> collection, boolean def_is_null){
            if(value instanceof LuaUserdata lud){
                return lud.m_instance instanceof List<?> list ? list : null;
            }else{
                return (List<?>)coerce(value,(Collection<Object>)collection,def_is_null);
            }
        }
        private static Set<?> coerce(LuaValue value, Set<Object> collection, boolean def_is_null){
            if(value instanceof LuaUserdata lud){
                return lud.m_instance instanceof Set<?> set ? set : null;
            }else{
                return (Set<?>)coerce(value,(Collection<Object>)collection,def_is_null);
            }
        }


        public static LuaValue coerce(Object obj){
            if(obj==null){
                return LuaValue.NIL;
            }else if(obj instanceof Map<?,?> map){
                return coerce(map);
            }else if(obj instanceof Collection<?> c){
                return coerce(c);
            }else{
                return CoerceJavaToLua.coerce(obj);
            }
        }
        public static LuaTable coerce(Map<?,?> map){
            int size=map.size();
            LuaTable table=new LuaTable(size,size);
            map.forEach((key,value)->table.set(coerce(key),coerce(value)));
            return table;
        }
        public static LuaTable coerce(Collection<?> c){
            LuaTable table=new LuaTable(c.size(),0);
            int i=0; for(Object o:c){table.set(i++,coerce(o));}
            return table;
        }
        public static LuaTable coerce(Object[] arr){
            LuaTable table=new LuaTable(arr.length,0);
            for(int i=0;i<arr.length;i++){
                table.set(i+1,coerce(arr[i]));
            }
            return table;
        }
    }
    public static class LuaJavaLib extends LuajavaLib{
        @Override
        protected Class<?> classForName(String name) throws ClassNotFoundException {
            return Class.forName(name, true, Thread.currentThread().getContextClassLoader());
        }
    }
    public static class Network{
        public static Throwable load(OkHttpClient client,String url, String domain, File file, Boolean cancel_flag, Callback2<Long,Long> listener){
            return load(client,url,domain,null,file,cancel_flag,listener);
        }
        public static Throwable load(OkHttpClient client,String url, LuaTable headers, File file, Boolean cancel_flag, Callback2<Long,Long> listener){
            return load(client,url,null,headers,file,cancel_flag,listener);
        }
        public static Throwable load(OkHttpClient client,String url, String domain, LuaTable headers, File file, Boolean cancel_flag, Callback2<Long,Long> listener){
            return NetworkUtils.load(client,url,extendHeaders(domain,url,Coercion.coerce(headers,Map.class)),file,cancel_flag,listener,false);
        }
        public static OkHttpClient getClient(){return getClient(false);}
        public static OkHttpClient getClient(boolean descramble){return NetworkUtils.getClient(descramble);}
        public static String getCookie(String domain,String name){
            return getCookie(NetworkUtils.getCookies(domain),name);
        }
        private static String getCookie(List<Cookie> cookies,String name){
            return cookies==null? null:cookies.stream().filter(cookie -> cookie.name().equals(name)).map(Cookie::value).findAny().orElse(null);
        }
        public static String decode(String encoded){
            return encoded==null? null:URLDecoder.decode(encoded);
        }
        public static String encode(String decoded){
            return decoded==null? null:URLEncoder.encode(decoded);
        }
        public static String load(String url, LuaTable headers) throws IOException {
            return load_as_String(url,headers,null);
        }
        public static String load_as_String(String url, LuaTable headers, LuaTable body) throws IOException {
            return load_as_String(url, Coercion.coerce(headers,Map.class), Coercion.coerce(body,Map.class));
        }
        public static String load_as_String(String url, LuaTable headers, String body,String type) throws IOException {
            return load_as_String(url, extendHeaders(null,url,Coercion.coerce(headers,Map.class)), RequestBody.create(body, MediaType.parse(type)));
        }
        public static String load_as_String(String url, Map<String,String> headers, Map<String,String> body) throws IOException {
            return load_as_String(url,extendHeaders(null,url,headers),convertBody(body));
        }
        public static String load_as_String(String url, Headers headers, RequestBody body) throws IOException {
            String answer;
            try {
                if(body==null){
                    answer=NetworkUtils.getString(url,headers);
                }else{
                    answer=NetworkUtils.getString(url,headers,body);
                }
            } catch (IOException e) {
                answer=e.getMessage();
                e.printStackTrace();
                if(e instanceof HttpStatusException || e instanceof SocketTimeoutException || e instanceof SSLException){
                    throw e;
                }
            }
            return answer;
        }
        public static Document load_as_Document(String url, LuaTable headers, LuaTable body) throws IOException {
            return Jsoup.parse(load_as_String(url,headers,body),url);
        }
        public static URLBuilder url_builder(String host){
            return new URLBuilder(host);
        }
        public static Document parse(String html){
            return Jsoup.parse(html);
        }
    }
    public static class Utils{
        public static Throwable write(File file,String text,boolean append){
            if(text==null){return null;}
            file.getParentFile().mkdirs();
            try{
                FileOutputStream stream=new FileOutputStream(file,append);
                stream.write(text.getBytes(StandardCharsets.UTF_8));
                stream.close();
                return null;
            }catch (Throwable e){
                return e;
            }
        }
        public static String unescape_unicodes(String escaped){
            return org.alex.kitsune.utils.Utils.unescape_unicodes(escaped);
        }
        public static Map<?,?> to_map(LuaTable table){
            return Coercion.coerce(table,Map.class);
        }
        public static List<?> to_list(LuaTable table){
            return Coercion.coerce(table,List.class);
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
        public static String attr(Elements elements, String attr,String defValue, String delimiter){return ((elements!=null && elements.size()>0) ? elements.stream().map(e->e.attr(attr)).filter(text->text.length()>0).collect(Collectors.joining(delimiter)) : defValue);}
        public static String attr(Elements elements, String attr, String defValue){return ((elements!=null && elements.size()>0) ? elements.attr(attr) : defValue);}
        public static String attr(Elements elements, String attr){return attr(elements, attr,null);}
        public static String text(Elements elements, String defText, String delimiter){return ((elements!=null && elements.size()>0) ? elements.stream().map(Element::text).filter(text->text.length()>0).collect(Collectors.joining(delimiter)) : defText);}
        public static String text(Elements elements, String defText){return ((elements!=null && elements.size()>0) ? elements.text() : defText);}
        public static String text(Elements elements){return text(elements,null);}
        public static List<Chapter> uniqueChapters(List<Chapter> chapters,boolean translator_with_max_chapters,String translator){
            HashMap<String,Integer> translators=new LinkedHashMap<>();
            int max=0; Integer value;
            String translator_max=translator;
            for (Chapter chapter:chapters) {
                String key=chapter.getTranslator();
                translators.put(key,value=((value=translators.getOrDefault(key,0))!=null?value:0)+1);
                if(value>max){max=value; translator_max=key;}
                else if(value==max && Objects.equals(key,translator)){
                    translator_max=translator;
                }
            }
            if(translator_with_max_chapters){translator=translator_max;}
            if(translators.size()>1){
                final HashMap<String,Chapter> map=new LinkedHashMap<>();
                for (Chapter chapter:chapters) {
                    String key=chapter.getVol()+"--"+chapter.getNum();
                    if(Objects.equals(translator,chapter.getTranslator())){
                        map.put(key,chapter);
                    }else{
                        map.putIfAbsent(key,chapter);
                    }
                }
                chapters.clear();
                chapters.addAll(map.values());
            }
            return chapters;
        }
        public static List<Chapter> uniqueChapters(List<Chapter> chapters, boolean translator_with_max_chapters){
            return uniqueChapters(chapters,translator_with_max_chapters,chapters.size()>0?chapters.get(0).getTranslator():null);
        }
    }

    @Override
    public Language getLanguageInterface() {
        return new LuaLanguage();
    }
}
