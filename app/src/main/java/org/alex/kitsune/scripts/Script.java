package org.alex.kitsune.scripts;

import com.alex.edittextcode.EditTextCode;
import org.alex.kitsune.utils.Utils;
import java.io.*;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Script {
    private final String name;
    private final String language;
    private final String text;
    private final String path;
    public Script(InputStream in,String name,String path,String language){
        this.name=name;
        this.text=Utils.File.readStream(in);
        this.path=path;
        this.language=language;
    }
    public Script(File script,String language) throws FileNotFoundException {
        this(new FileInputStream(script), script.getName(), script.getPath(), language);
    }
    public static Script getInstance(String script_path)throws Throwable{return getInstance(new File(script_path));}
    public static Script getInstance(File script) throws Throwable{
        String suffix=script.getName();
        suffix=suffix.substring(suffix.lastIndexOf('.')+1);
        switch(suffix){
            case Lua.extension: return new Lua(script);
            default: throw new Exception("Impossible to determine the language with suffix="+suffix+"\nEnable suffixes are: lua\nEnable languages: Lua");
        }
    }
    public static boolean checkSuffix(String str){
        String suffix=str.substring(str.lastIndexOf('.')+1);
        switch(suffix){
            case Lua.extension: return true;
            default: return false;
        }
    }
    public String getLanguage(){return this.language;}
    public String getText(){return text;}
    public String getName(){return name;}
    public String getPath(){return path;}
    public abstract void setSTDOUT(PrintStream out);
    public abstract void setSTDERR(PrintStream err);
    public abstract int getLanguageIconId();
    public abstract void put(String name,Object object);
    public abstract <T> T get(String name,Class<T> return_type);
    public <T> T get(String name,Class<T> return_type,T defValue){T value=get(name, return_type); return value!=null ? value:defValue;}
        public String getString(String name,String def){return get(name,String.class,def);}
        public boolean getBoolean(String name,boolean def){return get(name,Boolean.class,def);}
        public int getInt(String name,int def){return get(name,Integer.class,def);}
        public float getFloat(String name,float def){return get(name,Float.class,def);}
        public double getDouble(String name,double def){return get(name,Double.class,def);}
        public long getLong(String name,long def){return get(name,Long.class,def);}
    public abstract <T> T invokeMethod(Object object, String name, Class<T> return_type, Object... params);
    public <T> T invokeMethod(String name, Class<T> return_type, Object... params){return invokeMethod(null,name,return_type,params);}
    //public void invokeMethod(String name, Object object,Object... params){invokeMethod(name,object,Void.class,params);}
    public void invokeMethod(String name, Object... params){invokeMethod(name,Void.class,params);}
    public abstract EditTextCode.SyntaxHighlightRule[] getSyntaxHighlightRules();
    public abstract Set<String> getSuggestionsSet();
    protected final void addSuggestions(Set<String> set, Pattern p){
        Matcher m=p.matcher(p.pattern());
        while(m.find()){
            set.add(m.pattern().toString().substring(m.start(),m.end()));
        }
    }
}
