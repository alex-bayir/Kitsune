package org.alex.kitsune.commons;

import androidx.annotation.NonNull;
import com.alex.json.java.JSON;
import org.jetbrains.annotations.NotNull;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

public class URLBuilder {
    private final String url;
    private final LinkedHashMap<String,Object> params=new LinkedHashMap<>();
    public URLBuilder(String url){
        this.url=url;
    }
    public URLBuilder add(String key, Object value){
        if(key!=null && value!=null){
            if(value instanceof Object[] || value instanceof Collection<?>){
                params.put(key, value);
            }else{
                params.put(key,String.valueOf(value));
            }
        }
        return this;
    }
    public URLBuilder add(String key, Object[] values,String delimiter){
        if(key!=null && values!=null && values.length>0){
            add(key,join(delimiter,values));
        }
        return this;
    }
    public URLBuilder add(String[] keys,Object value){
        if(keys!=null && value!=null){
            for(String key:keys){
                add(key,value);
            }
        }
        return this;
    }
    public URLBuilder add(JSON.Object params){
        params.keySet().forEach(key->add(key,params.get(key)));
        return this;
    }
    public URLBuilder add(String param){
        if(param!=null){params.put(param,null);}
        return this;
    }

    public URLBuilder clearParams(){
        params.clear();
        return this;
    }

    private static String build(String key, Object value, boolean encode){
        if(value==null){
            return encode ? URLEncoder.encode(key) : key;
        }else if(value instanceof Object[] values){
            return Arrays.stream(values).filter(Objects::nonNull).filter(v->!String.valueOf(v).isEmpty()).map(v->encode?URLEncoder.encode(key)+"="+URLEncoder.encode(String.valueOf(v)):key+"="+v).collect(Collectors.joining("&"));
        }else if(value instanceof Collection<?> values){
            return values.stream().filter(Objects::nonNull).filter(v->!String.valueOf(v).isEmpty()).map(v->encode?URLEncoder.encode(key)+"="+URLEncoder.encode(String.valueOf(v)):key+"="+v).collect(Collectors.joining("&"));
        }else{
            return encode?URLEncoder.encode(key)+"="+URLEncoder.encode(String.valueOf(value)):key+"="+value;
        }
    }
    public String build(){
        return url+params.entrySet().stream().map(entry->build(entry.getKey(),entry.getValue(),true)).filter(s ->!s.isEmpty()).collect(Collectors.joining("&","?",""));
    }
    @NonNull
    @NotNull
    @Override
    public String toString() {
        return url+params.entrySet().stream().map(entry->build(entry.getKey(),entry.getValue(),false)).filter(s ->!s.isEmpty()).collect(Collectors.joining("&","?",""));
    }
    private String join(String delimiter,Object[] tokens){
        return Arrays.stream(tokens).map(String::valueOf).collect(Collectors.joining(delimiter));
    }
}
