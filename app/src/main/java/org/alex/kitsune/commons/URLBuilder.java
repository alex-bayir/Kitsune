package org.alex.kitsune.commons;

import androidx.annotation.NonNull;
import org.alex.json.JSON;
import org.jetbrains.annotations.NotNull;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

public class URLBuilder {
    private final String url;
    private final LinkedHashMap<String,String> params=new LinkedHashMap<>();
    public URLBuilder(String url){
        this.url=url;
    }
    public URLBuilder add(String key, Object value){
        if(key!=null && value!=null){
            if(value instanceof Object[] values){
                for(Object val:values){
                    add(key,val);
                }
            }else if(value instanceof Collection<?> values){
                for(Object val:values){
                    add(key,val);
                }
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

    public String build(){
        return url+params.entrySet().stream().map(entry->entry.getValue()!=null?entry.getKey()+"="+URLEncoder.encode(entry.getValue()):URLEncoder.encode(entry.getKey())).collect(Collectors.joining("&","?",""));
    }
    @NonNull
    @NotNull
    @Override
    public String toString() {
        return url+params.entrySet().stream().map(entry->entry.getValue()!=null?entry.getKey()+"="+entry.getValue():entry.getKey()).collect(Collectors.joining("&","?",""));
    }
    private String join(String delimiter,Object[] tokens){
        return Arrays.stream(tokens).map(String::valueOf).collect(Collectors.joining(delimiter));
    }
}
