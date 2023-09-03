package org.alex.kitsune.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;
import androidx.preference.PreferenceManager;
import org.alex.kitsune.BuildConfig;
import org.alex.kitsune.R;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.book.Book_Scripted;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.ui.shelf.Catalogs;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Utils;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.stream.Collectors;

public class BookService {
    public enum Type{
        All,
        History,
        Saved,
        Favorites,
        Size;
        Type(){}
    }
    private static String dir;
    private static final Hashtable<Integer, Book> map=new Hashtable<>();
    private static Hashtable<Integer, Book> mapHistory,mapSaved,mapFavorites;
    private static HashSet<String> categories;
    public static String defFavoriteCategory="Favorite";
    public static boolean isUpdating=false;
    private static String cacheDir;

    public static String init(Context context){
        clearCache(cacheDir=context.getExternalCacheDir().getAbsolutePath());
        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(context);
        NetworkUtils.setTimeout(prefs);
        dir=prefs.getString(Constants.saved_path,context.getExternalFilesDir("saved").getAbsolutePath());
        copyScriptsFromAssets(context);
        Book_Scripted.setScripts(Catalogs.getBookScripts(context.getExternalFilesDir(Constants.scripts)));
        Catalogs.init(prefs);
        update();
        return dir;
    }
    public static void copyScriptsFromAssets(Context context){
        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(context);
        try{
            String[] paths=context.getAssets().list("scripts");
            String dir=context.getExternalFilesDir(Constants.scripts).getAbsolutePath()+File.separator;
            for(String path : paths){
                if(!BuildConfig.VERSION_NAME.equals(prefs.getString(Constants.version,"")) || BuildConfig.BUILD_TYPE.equals("debug") || !new File(dir+path).exists()){
                    Utils.File.copy(context.getAssets().open("scripts"+File.separator+path),new FileOutputStream(dir+path));
                }
            }
            prefs.edit().putString(Constants.version,BuildConfig.VERSION_NAME).apply();
        }catch (Exception e){e.printStackTrace();}
    }
    public static void init(String new_dir){
        Utils.File.move(new File(dir),new File(dir=new_dir));
    }
    public static boolean isInited(){return dir!=null && getMap(BookService.Type.All).size()>0;}
    public static String getDir(){return dir;}
    public static String getCacheDir(){return cacheDir;}
    public static void setCacheDirIfNull(List<Book> books){for(Book book : books){if(book.getDir()==null){book.setDir(getCacheDir());}} replaceIfExists(books,map);}
    public static void setDir(List<Book> books){setDir(books,dir);}
    public static void setDir(List<Book> books, String dir){for(Book book : books){book.setDir(dir);}}
    public static Map<Integer, Book> getMap(Type type){
        return switch (type != null ? type : Type.All) {
            default -> map;
            case History -> mapHistory;
            case Size, Saved -> mapSaved;
            case Favorites -> mapFavorites;
        };
    }
    public static Map<Integer, Book> getAll(){return getMap(Type.All);}
    public static Book get(int hash, Type type){return getMap(type).get(hash);}
    public static Book get(int hash){return get(hash,Type.All);}

    public static HashSet<String> getCategories(){return categories;}
    public static int putNew(Book book){if(book !=null){book.setDir(cacheDir);} return put(map, book);}
    public static Book getOrPutNewWithDir(int hash, String json){
        Book book=get(hash);
        if(book==null && (book=Book.fromJSON(json))!=null){
            book.moveTo(dir);
            put(map, book);
        }
        return book;
    }
    public static Book getOrPutNewWithDir(int hash, Book book){
        Book m=get(hash);
        if(m==null && (m= book)!=null){
            m.moveTo(dir);
            put(map,m);
        }
        return m;
    }
    public static Book getOrPutNewWithDir(Book book){return book !=null ? getOrPutNewWithDir(book.hashCode(), book) : null;}

    public static void put(Book book){
        if(book!=null){
            map.put(book.hashCode(), book);
            allocate(book,true);
        }
    }
    private static int put(Map<Integer, Book> map, Book book){if(map!=null && book !=null){int hash; map.put(hash= book.hashCode(), book); return hash;} return -1;}
    private static Book remove(Map<Integer, Book> map, Book book){if(map!=null && book !=null){return map.remove(book.hashCode());} return null;}

    public static boolean allocate(Book book, boolean enableDelete){
        if(book !=null){
            if(book.getHistory()!=null){put(mapHistory, book); enableDelete=false;}else{mapHistory.remove(book.hashCode());}
            if(book.countSaved()>0){put(mapSaved, book); enableDelete=false;}else{mapSaved.remove(book.hashCode());}
            if(book.getCategory()!=null){put(mapFavorites, book); categories.add(book.getCategory()); enableDelete=false;}else{remove(mapFavorites, book);}
            if(enableDelete){
                book.delete();
            }
            return enableDelete;
        }else{
            return false;
        }
    }
    public synchronized static void update(){
        update(map,loadBookMap(dir));
        mapHistory=new Hashtable<>();
        mapSaved=new Hashtable<>();
        mapFavorites=new Hashtable<>();
        categories=new HashSet<>();
        categories.add(defFavoriteCategory);
        LinkedList<Integer> remove_list=new LinkedList<>();
        for(Map.Entry<Integer, Book> entry: map.entrySet()){
            Book book=entry.getValue();
            if(book==null || allocate(book,true)){
                remove_list.add(entry.getKey());
            }
        }
        for(Integer i:remove_list){map.remove(i);}
    }

    public static void update(int hash){update(map.get(hash));}
    public static void update(Book book){
        if(book !=null){
            book.updateDetails();
            allocate(book,false);
        }
    }

    public static void update(Map<Integer, Book> map, Map<Integer, Book> newmap){
        for(Map.Entry<Integer, Book> entry : newmap.entrySet()){
            if(entry.getValue()!=null){
                if(map.containsKey(entry.getKey())){
                    map.get(entry.getKey()).updateDetails(entry.getValue());
                }else{
                    map.put(entry.getKey(),entry.getValue());
                }
            }

        }
    }

    public static HashMap<Integer, Book> loadBookMap(String dir){
        HashMap<Integer, Book> map=new HashMap<>();
        File[] files=new File(dir).listFiles();
        if(files==null){return map;}
        for (File file : files) {
            int hash=put(map, Book.loadFromStorage(file.getAbsolutePath()+File.separator+"summary"));
            if(hash==-1){Utils.File.delete(file);}
        }
        return map;
    }

    public static ArrayList<Book> getSorted(Type type){
        return sort(new ArrayList<>(getMap(type).values()),type);
    }
    public static Set<Book> getSet(Type type){
        return new HashSet<>(getMap(type).values());
    }
    public static Comparator<Book> getComparator(Type type){
        return switch (type != null ? type : Type.All) {
            default -> Book.AlphabeticalComparator;
            case History -> Book.HistoryComparator;
            case Saved -> Book.SavingTimeComparator;
            case Favorites -> Book.CategoryTimeComparator;
            case Size -> Book.ImagesSizesComparator;
        };
    }
    public static ArrayList<Book> sort(ArrayList<Book> list, Type type){
        list.sort(getComparator(type));
        return list;
    }
    public static ArrayList<Book> getFavorites(String category){
        if(category==null){return null;}
        ArrayList<Book> list=getSorted(Type.Favorites);
        list.removeIf(book -> !category.equals(book.getCategory()));
    return list;}
    public static int getCountUpdated(){int count=0;for(Book book :getMap(Type.All).values()){if(book.isUpdated()){count++;}}return count;}
    public static boolean isAllUpdated(){return getMap(Type.All).size()==getCountUpdated();}

    public static ArrayList<Book> getWithNew(){
        ArrayList<Book> list=new ArrayList<>();
        for(Book book :getSorted(Type.All)){
            if(book.getNotCheckedNew()>0){
                list.add(book);
            }
        }
        return list;
    }

    public static <T extends List<Book>> T replaceIfExists(T destination, Map<Integer, Book> values){
        if(destination!=null && values!=null){
            destination.replaceAll(book -> values.getOrDefault(book.hashCode(), book));
        }
        return destination;
    }
    public static void clearCache(String cacheDir){
        Utils.File.delete(new File(cacheDir));
    }

    private static int p=-1;
    public static boolean isNotCalledUpdate(){return p<0;}
    public static void check_for_updates(Context context, Callback<Integer> onFinish){
        if(NetworkUtils.isNetworkAvailable(context)){
            p=0;
            List<Book> books=BookService.getMap(BookService.Type.All).values().stream().filter(book -> !book.isUpdated()).collect(Collectors.toList());
            final int size=books.size();
            BookService.isUpdating=true;
            for(Book book : books){
                book.update((updated)->{
                    if(updated){
                        BookService.setCacheDirIfNull(book.getSimilar());
                        context.sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash, book.hashCode()).putExtra("only info",true));
                        if(book.getNotCheckedNew()>0){context.sendBroadcast(new Intent(Constants.action_Update_New).putExtra(Constants.hash, book.hashCode()));}
                        if(++p==size){BookService.isUpdating=false; onFinish.call(BookService.getWithNew().size());}
                    }
                },throwable->{
                    if(++p==size){BookService.isUpdating=false; onFinish.call(BookService.getWithNew().size());}
                });
            }
        }else{
            Toast.makeText(context, R.string.no_internet,Toast.LENGTH_LONG).show();
        }
    }
}
