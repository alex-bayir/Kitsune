package org.alex.kitsune.commons;

import androidx.annotation.NonNull;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import java.util.Collection;

public class URLBuilder {
    private final String url;
    private final StringBuilder params=new StringBuilder();
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
                params.append(params.length()==0 ? '?' : '&').append(key).append('=').append(value);
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
    public URLBuilder add(JSONObject params){
        params.keys().forEachRemaining(key->add(key,params.opt(key)));
        return this;
    }
    public URLBuilder add(String param){
        if(param!=null){params.append(param);}
        return this;
    }

    public URLBuilder clearParams(){
        params.delete(0,params.length());
        return this;
    }

    public String build(){
        return toString()
                .replace(" ","%20")
                .replace("[","%5B")
                .replace("]","%5D");
    }
    @NonNull
    @NotNull
    @Override
    public String toString() {
        return url+params;
    }
    private String join(String delimiter,Object[] tokens){
        if (tokens.length==0){return "";}
        final StringBuilder sb = new StringBuilder();
        sb.append(tokens[0]);
        for (int i=1; i<tokens.length; i++) {
            sb.append(delimiter).append(tokens[i]);
        }
        return sb.toString();
    }
}
