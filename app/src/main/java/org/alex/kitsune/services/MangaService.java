package org.alex.kitsune.services;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import org.alex.kitsune.BuildConfig;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.manga.Manga_Scripted;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.ui.shelf.Catalogs;
import org.alex.kitsune.utils.Utils;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

public class MangaService {
    public enum Type{
        All,
        History,
        Saved,
        Favorites,
        Size;
        Type(){}
    }
    private static String dir;
    private static final Hashtable<Integer, Manga> map=new Hashtable<>();
    private static Hashtable<Integer, Manga> mapHistory,mapSaved,mapFavorites;
    private static HashSet<String> categories;
    public static String defFavoriteCategory="Favorite";
    public static boolean isUpdating=false;
    private static String cacheDir;

    public static String init(Context context){
        clearCache(cacheDir=context.getExternalCacheDir().getAbsolutePath());
        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(context);
        Catalogs.containers=Catalogs.getCatalogs(prefs);
        dir=prefs.getString(Constants.saved_path,context.getExternalFilesDir("saved").getAbsolutePath());
        copyScriptsFromAssets(context);
        Manga_Scripted.setScripts(Catalogs.getMangaScripts(context.getExternalFilesDir(Constants.manga_scripts)));
        update();
        return dir;
    }
    public static void copyScriptsFromAssets(Context context){
        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(context);
        try{
            String[] paths=context.getAssets().list("scripts");
            String dir=context.getExternalFilesDir(Constants.manga_scripts).getAbsolutePath()+"/";
            for(int i=0;i<paths.length;i++){
                if(!BuildConfig.VERSION_NAME.equals(prefs.getString(Constants.version,"")) || BuildConfig.BUILD_TYPE.equals("debug") || !new File(dir+paths[i]).exists()){
                    Utils.File.copy(context.getAssets().open("scripts/"+paths[i]),new FileOutputStream(dir+paths[i]));
                }
            }
            prefs.edit().putString(Constants.version,BuildConfig.VERSION_NAME).apply();
        }catch (Exception e){e.printStackTrace();}
    }
    public static void init(String new_dir){
        Utils.File.move(new File(dir),new File(dir=new_dir));
    }
    public static boolean isInited(){return dir!=null && getMap(MangaService.Type.All).size()>0;}
    public static String getDir(){return dir;}
    public static String getCacheDir(){return cacheDir;}
    public static void setCacheDirIfNull(List<Manga> mangas){for(Manga manga:mangas){if(manga.getDir()==null){manga.setDir(getCacheDir());}} replaceIfExists(mangas,map);}
    public static void setDir(List<Manga> mangas){setDir(mangas,dir);}
    public static void setDir(List<Manga> mangas,String dir){for(Manga manga:mangas){manga.setDir(dir);}}
    public static Map<Integer,Manga> getMap(Type type){
        switch (type!=null ? type : Type.All){
            default:
            case All: return map;
            case History: return mapHistory;
            case Size:
            case Saved: return mapSaved;
            case Favorites: return mapFavorites;
        }
    }
    public static Map<Integer,Manga> getAll(){return getMap(Type.All);}
    public static Manga get(int hash,Type type){return getMap(type).get(hash);}
    public static Manga get(int hash){return get(hash,Type.All);}

    public static HashSet<String> getCategories(){return categories;}
    public static int putNew(Manga manga){if(manga!=null){manga.setDir(cacheDir);} return put(map,manga);}
    public static Manga getOrPutNewWithDir(int hash,Manga manga){
        Manga m=get(hash);
        if(m==null && (m=manga)!=null){
            m.moveTo(dir);
            put(map,m);
        }
        return m;
    }
    public static Manga getOrPutNewWithDir(Manga manga){return manga!=null ? getOrPutNewWithDir(manga.hashCode(),manga) : null;}

    public static void put(Manga manga){
        if(manga!=null){
            map.put(manga.hashCode(), manga);
            allocate(manga,true);
        }
    }
    private static int put(Map<Integer,Manga> map,Manga manga){if(map!=null && manga!=null){int hash; map.put(hash=manga.hashCode(),manga); return hash;} return -1;}
    private static Manga remove(Map<Integer,Manga> map,Manga manga){if(map!=null && manga!=null){return map.remove(manga.hashCode());} return null;}

    public static boolean allocate(Manga manga,boolean enableDelete){
        if(manga!=null){
            if(manga.getHistory()!=null){put(mapHistory,manga); enableDelete=false;}else{mapHistory.remove(manga.hashCode());}
            if(manga.countSaved()>0){put(mapSaved,manga); enableDelete=false;}else{mapSaved.remove(manga.hashCode());}
            if(manga.getCategoryFavorite()!=null){put(mapFavorites,manga); categories.add(manga.getCategoryFavorite()); enableDelete=false;}else{remove(mapFavorites,manga);}
            if(enableDelete){
                manga.delete();
            }
            return enableDelete;
        }else{
            return false;
        }
    }
    public synchronized static void update(){
        update(map,loadMangaMap(dir));
        mapHistory=new Hashtable<>();
        mapSaved=new Hashtable<>();
        mapFavorites=new Hashtable<>();
        categories=new HashSet<>();
        categories.add(defFavoriteCategory);
        LinkedList<Integer> removelist=new LinkedList<>();
        for(Map.Entry<Integer,Manga> entry: map.entrySet()){
            Manga manga=entry.getValue();
            if(manga==null || allocate(manga,true)){
                removelist.add(entry.getKey());
            }
        }
        for(Integer i:removelist){map.remove(i);}
    }

    public static void update(int manga_hash){update(map.get(manga_hash));}
    public static void update(Manga manga){
        if(manga!=null){
            manga.updateDetails();
            allocate(manga,false);
        }
    }

    public static void update(Map<Integer,Manga> map,Map<Integer,Manga> newmap){
        for(Map.Entry<Integer,Manga> entry : newmap.entrySet()){
            if(entry.getValue()!=null){
                if(map.containsKey(entry.getKey())){
                    map.get(entry.getKey()).updateDetails(entry.getValue());
                }else{
                    map.put(entry.getKey(),entry.getValue());
                }
            }

        }
    }

    public static HashMap<Integer,Manga> loadMangaMap(String dir){
        HashMap<Integer,Manga> map=new HashMap<>();
        File[] files=new File(dir).listFiles();
        if(files==null){return map;}
        for (File file : files) {
            int hash=put(map,Manga.loadFromStorage(file.getAbsolutePath()+"/summary"));
            if(hash==-1){Utils.File.delete(file);}
        }
        return map;
    }

    public static ArrayList<Manga> getSorted(Type type){
        return sort(new ArrayList<>(getMap(type).values()),type);
    }
    public static Set<Manga> getSet(Type type){
        return new HashSet<>(getMap(type).values());
    }
    public static Comparator<Manga> getComparator(Type type){
        switch (type!=null ? type : Type.All){
            default:
            case All: return Manga.AlphabeticalComparator;
            case History: return Manga.HistoryComparator;
            case Saved: return Manga.SavingTimeComparator;
            case Favorites: return Manga.FavoriteTimeComparator;
            case Size: return Manga.ImagesSizesComparator;
        }
    }
    public static ArrayList<Manga> sort(ArrayList<Manga> list,Type type){
        Collections.sort(list,getComparator(type));
        return list;
    }
    public static ArrayList<Manga> getFavorites(String category){
        if(category==null){return null;}
        ArrayList<Manga> list=getSorted(Type.Favorites);
        list.removeIf(manga -> !category.equals(manga.getCategoryFavorite()));
    return list;}
    public static int getCountUpdated(){int count=0;for(Manga manga:getMap(Type.All).values()){if(manga.isUpdated()){count++;}}return count;}
    public static boolean isAllUpdated(){return getMap(Type.All).size()==getCountUpdated();}

    public static ArrayList<Manga> getWithNew(){
        ArrayList<Manga> list=new ArrayList<>();
        for(Manga manga:getSorted(Type.All)){
            if(manga.getNotCheckedNew()>0){
                list.add(manga);
            }
        }
        return list;
    }

    public static <T extends List<Manga>> T replaceIfExists(T destination, Map<Integer,Manga> values){
        if(destination!=null && values!=null){
            destination.replaceAll(manga -> values.getOrDefault(manga.hashCode(), manga));
        }
        return destination;
    }
    public static String getPathSourceIcon(String domain){return dir+"/icons/"+domain;}
    public static void clearCache(String cacheDir){
        Utils.File.delete(new File(cacheDir));
    }
}
