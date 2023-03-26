package org.alex.kitsune.scripts;

import com.alex.edittextcode.EditTextCode.SyntaxHighlightRule;
import okhttp3.Headers;
import okhttp3.RequestBody;
import org.alex.kitsune.R;
import com.alex.json.java.JSON;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
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
        public static String load(String url, LuaTable headers) throws HttpStatusException {
            return load_as_String(url,headers,null);
        }
        public static String load_as_String(String url, LuaTable headers, LuaTable body) throws HttpStatusException {
            return load_as_String(url, Coercion.coerce(headers,Map.class), Coercion.coerce(body,Map.class));
        }
        public static String load_as_String(String url, Map<String,String> headers, Map<String,String> body) throws HttpStatusException {
            return load_as_String(url,extendHeaders(headers),convertBody(body));
        }
        public static String load_as_String(String url, Headers headers, RequestBody body) throws HttpStatusException {
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
                if(e instanceof HttpStatusException hse){
                    throw hse;
                }
            }
            return answer;
        }
        public static Document load_as_Document(String url, LuaTable headers, LuaTable body) throws HttpStatusException {
            return Jsoup.parse(load_as_String(url,headers,body),url);
        }
        public static URLBuilder url_builder(String host){
            return new URLBuilder(host);
        }
    }
    public static class Utils{
        public static Map<?,?> to_map(LuaTable table){
            return Coercion.coerce(table,Map.class);
        }
        public static List<?> to_list(LuaTable table){
            return Coercion.coerce(table,List.class);
        }
        public static LuaValue to_lua(Object obj){
            return Coercion.coerce(obj);
        }
        public static LuaTable to_table(Map<?,?> map){
            return Coercion.coerce(map);
        }
        public static LuaTable to_table(Collection<?> collection){
            return Coercion.coerce(collection);
        }
        public static LuaTable to_table(Object[] arr){
            return Coercion.coerce(arr);
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

    public static final SyntaxHighlightRule[] syntaxHighlightRules=new SyntaxHighlightRule[]{
            new SyntaxHighlightRule("\\b(luajava)\\b",0xFF00FF00),
            new SyntaxHighlightRule("\\b(function|local|if|else|break|while|for|in|end|return)\\b",0xFFCC7832),
            new SyntaxHighlightRule("(?<=[:\\.]).+?(?=\\()",0xFFFCC56C),
            new SyntaxHighlightRule("\\b(_G|_VERSION|assert|collectgarbage|dofile|error|getmetatable|ipairs|load|loadfile|next|pairs|pcall|print|rawequal|rawget|rawlen|rawset|require|select|setmetatable|tonumber|tostring|type|xpcall|coroutine|coroutine.create|coroutine.isyieldable|coroutine.resume|coroutine.running|coroutine.status|coroutine.wrap|coroutine.yield|debug|debug.debug|debug.gethook|debug.getinfo|debug.getlocal|debug.getmetatable|debug.getregistry|debug.getupvalue|debug.getuservalue|debug.sethook|debug.setlocal|debug.setmetatable|debug.setupvalue|debug.setuservalue|debug.traceback|debug.upvalueid|debug.upvaluejoin|io|io.close|io.flush|io.input|io.lines|io.open|io.output|io.popen|io.read|io.stderr|io.stdin|io.stdout|io.tmpfile|io.type|io.write|file:close|file:flush|file:lines|file:read|file:seek|file:setvbuf|math.abs|math.acos|math.asin|math.atan|math.ceil|math.cos|math.deg|math.exp|math.floor|math.fmod|math.huge|math.log|math.max|math.maxinteger|math.min|math.mininteger|math.modf|math.pi|math.rad|math.random|math.randomseed|math.sin|math.sqrt|math.tan|math.tointeger|math.type|math.ult|os|os.clock|os.date|os.difftime|os.execute|os.exit|os.getenv|os.remove|os.rename|os.setlocale|os.time|os.tmpname|package|package.config|package.cpath|package.loaded|package.loadlib|package.path|package.preload|package.searchers|package.searchpath|string|string.byte|string.char|string.dump|string.find|string.format|string.gmatch|string.gsub|string.len|string.lower|string.match|string.pack|string.packsize|string.rep|string.reverse|string.sub|string.unpack|string.upper|table|table.concat|table.insert|table.move|table.pack|table.remove|table.sort|table.unpack|utf8|utf8.char|utf8.charpattern|utf8.codepoint|utf8.codes|utf8.len|byte|char|dump|find|format|gmatch|gsub|len|lower|match|pack|packsize|rep|reverse|sub|unpack|upper)\\b",0xFFCC542E),
            new SyntaxHighlightRule("[0-9]+[.]?[0-9]*",0xFF6896BB),
            new SyntaxHighlightRule("(\\\".*?\\\")",0xFF6A864E),
            new SyntaxHighlightRule("--(.*)",0xFF808080),
            new SyntaxHighlightRule("---(.*)",0xFF609448),
            new SyntaxHighlightRule("--\\[\\[[\\s\\S]*--\\]\\]",0xFF808080)
    };
    @Override
    public SyntaxHighlightRule[] getSyntaxHighlightRules() {return syntaxHighlightRules;}

    @Override
    public Set<String> getSuggestionsSet() {
        HashSet<String> set=new HashSet<>();
        addSuggestions(set,syntaxHighlightRules[0].getPattern());
        addSuggestions(set,syntaxHighlightRules[1].getPattern());
        addSuggestions(set,syntaxHighlightRules[2].getPattern());
        return set;
    }

}
