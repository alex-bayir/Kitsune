package org.alex.kitsune.scripts;

import com.alex.edittextcode.EditTextCode.SyntaxHighlightRule;
import org.alex.kitsune.R;
import java.io.*;
import java.util.HashSet;
import java.util.Set;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.*;

public class Lua extends Script{
    public static final String extension="lua";
    private final Globals globals;
    public Lua(File script) throws Throwable {
        super(script, "Lua");
        globals=JsePlatform.standardGlobals();
        globals.load(new LuaJavaLib());
        globals.load("package.path=package.path..';"+(script.getParent()+File.separator).replace(File.separator,File.separator+File.separator)+"?.lua'").call();
        globals.loadfile(script.getAbsolutePath()).call();
        //globals.STDOUT=System.out;
        //globals.STDERR=System.err;
    }
    public Globals getGlobals(){return globals;}

    @Override
    public void setSTDOUT(PrintStream out){globals.STDOUT=out;}

    @Override
    public void setSTDERR(PrintStream err){globals.STDERR=err;}

    @Override
    public int getLanguageIconId(){return R.drawable.ic_lua;}

    @Override
    public void put(String name, Object object) {
        globals.set(name, CoerceJavaToLua.coerce(object));
    }

    @Override
    public <T> T get(String name,Class<T> return_type) {
        try{
            return (T)CoerceLuaToJava.coerce(globals.get(name), return_type);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public <T> T invokeMethod(Object object, String name, Class<T> return_type, Object... params) {
        LuaValue[] values=new LuaValue[params.length];
        for(int i=0;i<values.length;i++){values[i]=CoerceJavaToLua.coerce(params[i]);}
        if(return_type==null || return_type==Void.class){
            globals.get(name).invoke(values);
            return null;
        }else{
            return (T)CoerceLuaToJava.coerce(globals.get(name).invoke(values).arg1(), return_type);
        }
    }

    public static class LuaJavaLib extends LuajavaLib{
        @Override
        protected Class classForName(String name) throws ClassNotFoundException {
            return Class.forName(name, true, Thread.currentThread().getContextClassLoader());
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
