package org.alex.kitsune.ui.shelf;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.ArraySet;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import com.alex.listitemview.ListItemView;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import org.alex.kitsune.R;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.manga.Manga_Scripted;
import org.alex.kitsune.services.MangaService;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class StatisticsFragment extends Fragment implements MenuProvider {
    View root;
    ListItemView av_time;
    SharedPreferences prefs;
    BarChart chart;
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        requireActivity().removeMenuProvider(this);
        requireActivity().addMenuProvider(this);
        if(root!=null){return root;}
        root=inflater.inflate(R.layout.activity_statistics,container,false);
        prefs=PreferenceManager.getDefaultSharedPreferences(root.getContext());
        av_time=root.findViewById(R.id.average_read_time);
        styling(chart=root.findViewById(R.id.genres_statistics));
        root.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> chart.setMinimumHeight(root.getHeight()-av_time.getHeight()));
        bind(true);
        return root;
    }


    private void bind(){
        av_time.setSubtitle(time_to_string(av_time.getResources(),(int)(Statistics.getAverageTimeReading(prefs)/1000)));
    }
    private void bind(boolean all){
        Statistics.setGenresStatistics(root.findViewById(R.id.genres_statistics),all,root.getContext().getExternalFilesDir(null).getAbsolutePath()+"/statistics.json");
        bind();
    }

    private String time_to_string(Resources res, int time) {
        int hours=time/3600, minutes=(time%3600)/60, seconds=time%60;
        return (res.getQuantityString(R.plurals.hour, hours, hours) + " " + res.getQuantityString(R.plurals.minute, minutes, minutes)+ " " + res.getQuantityString(R.plurals.second, seconds, seconds));
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getActivity()!=null){getActivity().invalidateOptionsMenu();}
        bind();
    }

    public static void styling(BarChart chart){
        chart.setDrawValueAboveBar(true);
        chart.getDescription().setEnabled(false);
        chart.setExtraBottomOffset(-10);
        XAxis x=chart.getXAxis();
        x.setDrawAxisLine(false);
        x.setDrawGridLines(false);
        x.setGranularity(1f);
        x.setGranularityEnabled(true);
        x.setTextColor(Color.RED);
        x.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        x.setLabelCount(25);
        YAxis yl=chart.getAxisLeft();
        yl.setDrawAxisLine(false);
        yl.setAxisMinimum(0);
        yl.setTextColor(Color.GREEN);
        YAxis yr=chart.getAxisRight();
        yr.setDrawAxisLine(false);
        yr.setAxisMinimum(0);
        yr.setTextColor(Color.GREEN);
        chart.setOnTouchListener(new View.OnTouchListener() {
            float ly=0; final float round=0.001f;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP: v.getParent().requestDisallowInterceptTouchEvent(false); break;
                    case MotionEvent.ACTION_DOWN: ly=event.getY(); v.getParent().requestDisallowInterceptTouchEvent(!(chart.getLowestVisibleX()-round<chart.getXAxis().getAxisMinimum() && chart.getXAxis().getAxisMaximum()<chart.getHighestVisibleX()+round)); break;
                    case MotionEvent.ACTION_MOVE: float y=event.getY(); v.getParent().requestDisallowInterceptTouchEvent(!(y-ly<0 ? chart.getLowestVisibleX()-round<=chart.getXAxis().getAxisMinimum() : chart.getHighestVisibleX()+round>=chart.getXAxis().getAxisMaximum())); ly=y; break;
                }
                return false;
            }
        });
    }


    @Override
    public void onCreateMenu(@NonNull @NotNull Menu menu, @NonNull @NotNull MenuInflater inflater) {}

    @Override
    public void onPrepareMenu(@NonNull @NotNull Menu menu) {
        menu.findItem(R.id.action_find_manga).setVisible(false);
        menu.findItem(R.id.full).setVisible(true);
        menu.findItem(R.id.check_for_updates).setVisible(false);
        menu.findItem(R.id.action_add_source).setVisible(false);
        menu.findItem(R.id.action_update_sctips).setVisible(false);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull @NotNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.full: bind(item.setChecked(!item.isChecked()).isChecked()); return true;
        }
        return false;
    }


    public static class Statistics {

        public static TreeMap<String, Set<Integer>> byGenres(Set<String> genres, Set<Manga> mangas, boolean all){
            return applyByGenres(new TreeMap<>(String.CASE_INSENSITIVE_ORDER.reversed()),genres,mangas,all);
        }
        public static <T extends Map<String, Set<Integer>>> T applyByGenres(T map, Set<String> genres, Set<Manga> mangas, boolean all){
            if(all){for(String key:genres){map.putIfAbsent(key,new ArraySet<>());}}
            mangas.removeIf(manga -> {
                int hashCode=manga.hashCode();
                for(Set<Integer> set:map.values()){if(set.contains(hashCode)){return true;}}
                return false;
            });
            for(Manga manga:mangas){
                for(String word:genres){
                    if(word==null || word.length()==0){continue;}
                    if(Pattern.compile(word.replaceAll("[\\\\\\.\\*\\+\\-\\?\\^\\$\\|\\(\\)\\[\\]\\{\\}]","\\\\W"), Pattern.CASE_INSENSITIVE).matcher(manga.getGenres()).find()){
                        map.computeIfAbsent(word, k -> new ArraySet<>()).add(manga.hashCode());
                    }
                }
            }
            return map;
        }
        public static JSONObject toJSON(Map<String, Set<Integer>> map){return new JSONObject(map);}
        public static TreeMap<String, Set<Integer>> fromJSON(JSONObject json){return fromJSON(json,true);}
        public static TreeMap<String, Set<Integer>> fromJSON(JSONObject json, boolean full){
            TreeMap<String, Set<Integer>> map=new TreeMap<>(String.CASE_INSENSITIVE_ORDER.reversed());
            Iterator<String> it=json.keys();
            while(it.hasNext()){
                try{
                    String key=it.next();
                    JSONArray arr=json.getJSONArray(key);
                    ArraySet<Integer> set=new ArraySet<>(arr.length());
                    for(int i=0;i<arr.length();i++){
                        set.add(arr.optInt(i));
                    }
                    if(set.size()>0 || full){
                        map.put(key,set);
                    }
                }catch (JSONException e){e.printStackTrace();}
            }
            return map;
        }
        public static TreeMap<String, Set<Integer>> fromJSON(String json, boolean full) throws JSONException {
            return fromJSON(new JSONObject(json),full);
        }
        public static TreeMap<String, Set<Integer>> fromJSON(String json) throws JSONException {
            return fromJSON(new JSONObject(json));
        }
        public static <T extends Map<String, Set<Integer>>> T saveGenresStatistics(String path,T map){
            try{Utils.File.writeFile(new File(path),toJSON(map).toString(),false); return map;}catch (FileNotFoundException e){e.printStackTrace(); return map;}
        }
        public static  TreeMap<String, Set<Integer>> loadGenresStatistics(String path, boolean all, boolean save){
            return save ? saveGenresStatistics(path,loadGenresStatistics(path, all)):loadGenresStatistics(path, all);
        }
        public static TreeMap<String, Set<Integer>> loadGenresStatistics(String path, boolean all){
            return loadGenresStatistics(path, Manga_Scripted.getAllGenres(), MangaService.getSet(MangaService.Type.History),all);
        }
        public static TreeMap<String, Set<Integer>> loadGenresStatistics(String path, Set<String> genres, Set<Manga> mangas, boolean all){
            try{
                return applyByGenres(fromJSON(Utils.File.readFile(new File(path)),all),genres,mangas,all);
            }catch (FileNotFoundException | JSONException e){
                e.printStackTrace();
                return byGenres(genres,mangas,all);
            }
        }

        public static List<BarEntry> transform(Map<String, Set<Integer>> map){
            List<BarEntry> entries=new ArrayList<>(map.size());
            int x=0;
            for(Map.Entry<String, Set<Integer>> entry: map.entrySet()){
                entries.add(new BarEntry(x++,entry.getValue().size()));
            }
            return entries;
        }

        public static class IntValueFormatter extends ValueFormatter {
            @Override public String getFormattedValue(float value) {
                return String.valueOf((int)value);
            }
        }

        public static void setGenresStatistics(BarChart chart,boolean full,String path){
            new Thread(()->{
                TreeMap<String, Set<Integer>> statistics=loadGenresStatistics(path,full,path!=null);
                BarDataSet dataSet=new BarDataSet(transform(statistics),null);
                dataSet.setColor(Color.TRANSPARENT);
                dataSet.setBarBorderWidth(1f);
                dataSet.setBarBorderColor(Color.MAGENTA);
                BarData data=new BarData(dataSet);
                data.setValueFormatter(new IntValueFormatter());
                data.setBarWidth(0.9f);
                data.setValueTextColor(Color.GREEN);
                chart.setData(data);
                chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(statistics.keySet()));
                chart.getAxisLeft().setAxisMinimum(0);
                chart.getAxisRight().setAxisMinimum(0);
                chart.setVisibleXRangeMinimum(chart.getXAxis().getLabelCount());
                //chart.setVisibleXRange(0,chart.getXAxis().getLabelCount()*2);
                new Handler(Looper.getMainLooper()).post(()-> chart.animateY(2000));
            }).start();

        }


        public static void updateReading(SharedPreferences prefs,long new_time){
            JSONObject json=new JSONObject();
            try{
                json=new JSONObject(prefs.getString(Constants.reading_time,null));
            }catch (Throwable ignored){}
            long all_time=json.optLong("all time");
            long today_time=json.optLong("today time");
            long days=json.optLong("days",0);
            String today=json.optString("today");
            String date=new SimpleDateFormat("dd.MM.yyyy",Locale.getDefault(Locale.Category.FORMAT)).format(Calendar.getInstance().getTime());
            if(date.equals(today)){
                today_time+=new_time;
                all_time+=new_time;
            }else{
                today=date;
                all_time+=new_time;
                today_time=new_time;
                days+=1;
            }
            try{
                json.put("all time",all_time).put("today time",today_time).put("days",days).put("today",today);
            }catch (Throwable e){
                e.printStackTrace();
            }
            prefs.edit().putString(Constants.reading_time,json.toString()).apply();
        }
        public static long getAverageTimeReading(SharedPreferences prefs){
            JSONObject json=new JSONObject();
            try{
                json=new JSONObject(prefs.getString(Constants.reading_time,null));
            }catch (Throwable ignored){}
            return json.optLong("all time")/json.optLong("days",1);
        }

    }
}
