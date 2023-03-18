package org.alex.json;

import androidx.annotation.NonNull;
import org.jetbrains.annotations.NotNull;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public interface JSON {
    boolean isObject();
    boolean isArray();
    Object object();
    Array<?> array();
    Writer json(Writer writer,int spaces) throws IOException;
    String json(int spaces) throws IOException;
    File json(File file,int spaces) throws IOException;
    final class Object extends TreeMap<String, java.lang.Object> implements JSON {
        public Object(){}
        public Object(Map<String,?> map){
            if(map!=null){
                putAll(map);
            }
        }
        public static Object create(String json) throws IOException {
            return create(new StringReader(json));
        }
        public static Object create(File json) throws IOException {
            return create(new FileReader(json));
        }
        public static Object create(Reader reader) throws IOException {
            return JSON.json(reader).object();
        }
        @NonNull
        @Override public Object put(String key, java.lang.Object value){
            if(key!=null){
                if(value!=null){
                    super.put(key,value);
                }else{
                    super.remove(key);
                }
            }
            return this;
        }
        @Override public void putAll(@NonNull Map<? extends String, ?> m) {
            for(Entry<? extends String, ?> entry:m.entrySet()){
                put(entry.getKey(),entry.getValue());
            }
        }
        public java.lang.Object get(String key,java.lang.Object def){
            java.lang.Object value=get(key);
            return value!=null ? value : def;
        }
        public Object get(String key,Object def){
            java.lang.Object value=get(key);
            return value instanceof Object v ? v : def;
        }
        public Object getObject(String key){
            return get(key,(Object) null);
        }
        public Array<?> get(String key,Array<?> def){
            java.lang.Object value=get(key);
            return value instanceof Array<?> v ? v : def;
        }
        public Array<?> getArray(String key){
            return get(key,(Array<?>) null);
        }
        public String get(String key,String def){
            java.lang.Object value=get(key);
            return value instanceof String v ? v : def;
        }
        public String getString(String key){
            return get(key,(String)null);
        }
        public int get(String key,int def){
            java.lang.Object value=get(key);
            return value instanceof Number v ? v.intValue() : (value instanceof String str ? Integer.parseInt(str):def);
        }
        public int getInt(String key){
            return get(key,0);
        }
        public long get(String key,long def){
            java.lang.Object value=get(key);
            return value instanceof Number v ? v.longValue() : (value instanceof String str ? Long.parseLong(str):def);
        }
        public long getLong(String key){
            return get(key,0L);
        }
        public double get(String key,double def){
            java.lang.Object value=get(key);
            return value instanceof Number v ? v.doubleValue() : (value instanceof String str ? Double.parseDouble(str):def);
        }
        public double getDouble(String key){
            return get(key,0.0);
        }
        public float get(String key,float def){
            java.lang.Object value=get(key);
            return value instanceof Number v ? v.floatValue() : (value instanceof String str ? Float.parseFloat(str):def);
        }
        public float getFloat(String key){
            return get(key,0.0f);
        }
        public boolean get(String key,boolean def){
            java.lang.Object value=get(key);
            return value instanceof Boolean v ? v : (value instanceof String str ? Boolean.parseBoolean(str):def);
        }

        public String json(int spaces){
            String json=null;
            try{json=json(new StringWriter(),spaces).toString();}catch (IOException e){e.printStackTrace();}
            return json;
        }
        public File json(File json,int spaces) throws IOException {
            json(new FileWriter(json),spaces); return json;
        }
        public Writer json(Writer writer,int spaces) throws IOException {
            String line=spaces>0?"\n"+new String(new char[spaces-1]).replace("\0", "\t"):"";
            String tab=spaces>0?"\n"+new String(new char[spaces]).replace("\0", "\t"):"";
            boolean delimiter=false;
            writer.write('{');
            for(Map.Entry<String,java.lang.Object> entry:entrySet()){
                if(delimiter){writer.write(',');}
                writer.write(tab);
                writer.write('"');
                writer.write(entry.getKey());
                writer.write('"');
                writer.write(':');
                JSON.json(writer,entry.getValue(),spaces);
                delimiter=true;
            }
            writer.write(line);
            writer.write('}');
            return writer;
        }

        @Override
        public boolean isObject() {
            return true;
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public Object object() {
            return this;
        }

        @Override
        public Array<?> array() {
            return null;
        }

        @NonNull
        @Override
        public String toString() {
            return json(0);
        }
        public <T> Map<String,T> filter(Class<T> c){
            return JSON.filter(this,String.class, c);
        }
    }
    private static String escape(String s){
        return s.replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\f", "\\f")
                .replace("\"", "\\\"");
    }

    final class Array<T> extends LinkedList<T> implements JSON{
        public Array(){}
        public Array(Collection<T> array){
            if(array!=null){
                addAll(array);
            }
        }
        public Array(T[] array){
            this(array==null ? null : Arrays.asList(array));
        }
        public static Array<?> create(String json) throws IOException {
            return create(new StringReader(json));
        }
        public static Array<?> create(File json) throws IOException {
            return create(new FileReader(json));
        }
        public static Array<?> create(Reader reader) throws IOException {
            return JSON.json(reader).array();
        }

        public Array<T> put(T obj){add(obj); return this;}

        public java.lang.Object get(int pos,java.lang.Object def){
            java.lang.Object value=get(pos);
            return value!=null ? value : def;
        }
        public Object get(int pos,Object def){
            java.lang.Object value=get(pos);
            return value instanceof Object v ? v : def;
        }
        public Object getObject(int pos){
            return get(pos,(Object) null);
        }
        public Array<?> get(int pos,Array<?> def){
            java.lang.Object value=get(pos);
            return value instanceof Array<?> v ? v : def;
        }
        public Array<?> getArray(int pos){
            return get(pos,(Array<?>) null);
        }
        public String get(int pos,String def){
            java.lang.Object value=get(pos);
            return value instanceof String v ? v : def;
        }
        public String getString(int pos){
            return get(pos,(String)null);
        }
        public int get(int pos,int def){
            java.lang.Object value=get(pos);
            return value instanceof Number v ? v.intValue() : (value instanceof String str ? Integer.parseInt(str):def);
        }
        public int getInt(int pos){
            return get(pos,0);
        }
        public long get(int pos,long def){
            java.lang.Object value=get(pos);
            return value instanceof Number v ? v.longValue() : (value instanceof String str ? Long.parseLong(str):def);
        }
        public long getLong(int pos){
            return get(pos,0L);
        }
        public double get(int pos,double def){
            java.lang.Object value=get(pos);
            return value instanceof Number v ? v.doubleValue() : (value instanceof String str ? Double.parseDouble(str):def);
        }
        public double getDouble(int pos){
            return get(pos,0.0);
        }
        public float get(int pos,float def){
            java.lang.Object value=get(pos);
            return value instanceof Number v ? v.floatValue() : (value instanceof String str ? Float.parseFloat(str):def);
        }
        public float getFloat(int pos){
            return get(pos,0.0f);
        }

        public boolean get(int pos,boolean def){
            java.lang.Object value=get(pos);
            return value instanceof Boolean v ? v : (value instanceof String str ? Boolean.parseBoolean(str):def);
        }
        public String json(int spaces){
            String json=null;
            try{json=json(new StringWriter(),spaces).toString();}catch (IOException e){e.printStackTrace();}
            return json;
        }
        public File json(File json,int spaces) throws IOException {
            json(new FileWriter(json),spaces); return json;
        }
        public Writer json(Writer writer,int spaces) throws IOException {
            String line=spaces>0?"\n"+new String(new char[spaces-1]).replace("\0", "\t"):"";
            String tab=spaces<=0?"":"\n"+new String(new char[spaces]).replace("\0", "\t");
            boolean delimiter=false;
            writer.write('[');
            for(java.lang.Object value:this){
                if(delimiter){writer.write(',');}
                writer.write(tab);
                JSON.json(writer,value,spaces);
                delimiter=true;
            }
            writer.write(line);
            writer.write(']');
            return writer;
        }

        @Override
        public boolean isObject() {
            return false;
        }
        @Override
        public boolean isArray() {
            return true;
        }
        @Override
        public Object object() {
            return null;
        }
        @Override
        public Array<?> array() {
            return this;
        }
        @NonNull
        @NotNull
        @Override
        public String toString() {
            return json(0);
        }

        public String join(String delimiter, String... keys){
            return stream().map(value->{
                java.lang.Object v=value;
                for(String key:keys){
                    v=v instanceof Object o? o.get(key):null;
                }
                return v instanceof String s? s:null;
            }).filter(Objects::nonNull).collect(Collectors.joining(delimiter));
        }
        public <E> List<E> filter(Class<E> c){
            return JSON.filter(this,c);
        }
    }
    static <E,T> List<E> filter(List<T> list,Class<E> c){
        return list==null ? null : list.stream().filter(c::isInstance).map(c::cast).collect(Collectors.toList());
    }
    static <T> Map<String,T> filter(Map<?,?> map,Class<T> c){
        return JSON.filter(map,String.class, c);
    }
    static <K,V> Map<K,V> filter(Map<?,?> map,Class<K> k,Class<V> v){
        return map==null ? null : map.entrySet().stream().filter(e->k.isInstance(e.getKey()) && v.isInstance(e.getValue())).collect(Collectors.toMap(e->(K)e.getKey(),e->(V)e.getValue()));
    }
    static JSON json(String json) throws IOException{
        return json(new StringReader(json));
    }
    static JSON json(File json) throws IOException{
        return json(new FileReader(json));
    }
    static JSON json(Reader reader) throws IOException{
        return json(reader,null);
    }
    private static JSON json(Reader reader,JSON o) throws IOException {
        char c;
        boolean rs=false;
        boolean rv=o instanceof Array<?>;
        boolean vs=false;
        boolean vd=false;
        StringBuilder buffer=new StringBuilder();
        String key=null;
        while ((c=(char)reader.read())!=(char)-1){
            switch (c){
                case '{'->{
                    if(!rs){
                        if(o instanceof Object obj){
                            obj.put(key,json(reader,new Object())); key=null;
                        }else if(o instanceof Array arr){
                            arr.add(json(reader,new Object()));
                        }else{
                            o=new Object();
                        }
                    }
                }
                case '['->{
                    if(!rs){
                        if(o instanceof Object obj){
                            obj.put(key,json(reader,new Array<>())); key=null;
                        }else if(o instanceof Array arr){
                            arr.add(json(reader,new Array<>()));
                        }else{
                            o=new Array<>(); rv=true;
                        }
                    }
                }
                case ':'->{
                    if(!rs){
                        buffer.delete(0,buffer.length()); // clear
                        rv=true; vs=false;
                    }else{
                        buffer.append(c);
                    }
                }
                case '"'->{
                    if(key==null && o instanceof Object){
                        rs=!rs;
                        if(!rs){
                            key=buffer.toString();
                        }
                    }else{
                        if(rv){
                            rs=!rs; vs=true;
                        }
                    }
                }
                case '\\'->{
                    buffer.append(
                            switch ((char)reader.read()){
                                case '\"'->'"';
                                case '\\'->'\\';
                                case 't'->'\t';
                                case 'b'->'\b';
                                case 'n'->'\n';
                                case 'r'->'\r';
                                case 'f'->'\f';
                                default -> c;
                            }
                    );
                }
                case '.'->{if(rv){vd=true;} buffer.append(c);}
                case ','->{
                    if(rs){
                        buffer.append(c);
                    }else if(buffer.length()>0 || vs){
                        if(vs){
                            if(o instanceof Object obj){
                                obj.put(key,buffer.toString());
                            }else if(o instanceof Array arr){
                                arr.add(buffer.toString());
                            }
                        }else{
                            java.lang.Object value; String buf=buffer.toString();
                            switch (buf){
                                case "null"->value=null;
                                case "false"->value=false;
                                case "true"->value=true;
                                default -> {
                                    if(vd){
                                        value=Double.parseDouble(buf);
                                    }else{
                                        value=Long.parseLong(buf);
                                        if(Integer.MIN_VALUE<=(Long)value && (Long)value<=Integer.MAX_VALUE){
                                            value=Integer.parseInt(buf);
                                        }
                                    }
                                }
                            }
                            if(o instanceof Object obj){
                                obj.put(key,value);
                            }else if(o instanceof Array arr){
                                arr.add(value);
                            }
                        }
                        rv=o instanceof Array; vs=false; vd=false; key=null;
                        buffer.delete(0,buffer.length()); // clear
                    }
                }
                case '}',']'-> {
                    if(rs){
                        buffer.append(c);
                    }else{
                        if(vs){
                            if(o instanceof Object obj){
                                obj.put(key,buffer.toString());
                            }else if(o instanceof Array arr){
                                arr.add(buffer.toString());
                            }
                        }else if(buffer.length()>0 || vs){
                            java.lang.Object value; String buf=buffer.toString();
                            switch (buf){
                                case "null"->value=null;
                                case "false"->value=false;
                                case "true"->value=true;
                                default -> {
                                    if(vd){
                                        value=Double.parseDouble(buf);
                                    }else{
                                        value=Long.parseLong(buf);
                                        if(Integer.MIN_VALUE<=(Long)value && (Long)value<=Integer.MAX_VALUE){
                                            value=Integer.parseInt(buf);
                                        }
                                    }
                                }
                            }
                            if(o instanceof Object obj){
                                obj.put(key,value);
                            }else if(o instanceof Array arr){
                                arr.add(value);
                            }
                        }
                        return o;
                    }
                }
                case '\n','\t',' ','\r'->{if(rs){buffer.append(c);}}
                default -> {buffer.append(c);}
            }
        }
        throw new IllegalArgumentException("No ending tag found");
    }
    private static void json(Writer writer, java.lang.Object value, int spaces) throws IOException {
        if(value instanceof Object obj){
            obj.json(writer,spaces>0?spaces+1:0);
        }else if(value instanceof Array<?> arr){
            arr.json(writer,spaces>0?spaces+1:0);
        }else if(value instanceof Number || value instanceof Boolean){
            writer.write(String.valueOf(value));
        }else if(value instanceof Map<?,?> map){
            new Object(JSON.filter(map,String.class)).json(writer,spaces>0?spaces+1:0);
        }else if(value instanceof Collection<?> arr){
            new Array<>(arr).json(writer,spaces>0?spaces+1:0);
        }else if(value instanceof java.lang.Object[] arr){
            new Array<>(arr).json(writer,spaces>0?spaces+1:0);
        }else if(value instanceof int[] arr){
            new Array<>(toObject(arr)).json(writer,spaces>0?spaces+1:0);
        }else if(value instanceof long[] arr){
            new Array<>(toObject(arr)).json(writer,spaces>0?spaces+1:0);
        }else if(value instanceof double[] arr){
            new Array<>(toObject(arr)).json(writer,spaces>0?spaces+1:0);
        }else if(value instanceof float[] arr){
            new Array<>(toObject(arr)).json(writer,spaces>0?spaces+1:0);
        }else if(value instanceof boolean[] arr){
            new Array<>(toObject(arr)).json(writer,spaces>0?spaces+1:0);
        }else if(value instanceof char[] arr){
            new Array<>(toObject(arr)).json(writer,spaces>0?spaces+1:0);
        }else if(value instanceof byte[] arr){
            new Array<>(toObject(arr)).json(writer,spaces>0?spaces+1:0);
        }else if(value instanceof short[] arr){
            new Array<>(toObject(arr)).json(writer,spaces>0?spaces+1:0);
        }else if(value!=null){
            writer.write('\"');
            writer.write(escape(String.valueOf(value)));
            writer.write('\"');
        }else{
            writer.write("null");
        }
    }


    private static Boolean[] toObject(final boolean[] array) {
        if (array == null) {return null;}
        final Boolean[] result = new Boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = (array[i] ? Boolean.TRUE : Boolean.FALSE);
        }
        return result;
    }
    private static Byte[] toObject(final byte[] array) {
        if (array == null) {return null;}
        final Byte[] result = new Byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }
    private static Character[] toObject(final char[] array) {
        if (array == null) {return null;}
        final Character[] result = new Character[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }
    private static Double[] toObject(final double[] array) {
        if (array == null) {return null;}
        final Double[] result = new Double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }
    private static Float[] toObject(final float[] array) {
        if (array == null) {return null;}
        final Float[] result = new Float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }
    private static Integer[] toObject(final int[] array) {
        if (array == null) {return null;}
        final Integer[] result = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }
    private static Long[] toObject(final long[] array) {
        if (array == null) {return null;}
        final Long[] result = new Long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }
    private static Short[] toObject(final short[] array) {
        if (array == null) {return null;}
        final Short[] result = new Short[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }
}
