package org.alex.kitsune.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import org.alex.kitsune.Activity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;
import androidx.recyclerview.widget.RecyclerView;
import com.alex.listitemview.ListItemView;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.ui.main.MainActivity;
import org.alex.kitsune.services.BookService;
import org.alex.kitsune.R;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.logs.LogsActivity;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Utils;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;

public class SettingsActivity extends Activity {
    public static final String KEY="TYPE";
    public static final String TYPE_SHELF=SettingsFragment.Type.Shelf.name();
    public static final String TYPE_READER=SettingsFragment.Type.Reader.name();
    public static final String TYPE_GENERAL=SettingsFragment.Type.General.name();
    Toolbar toolbar;
    SettingsFragment.Type type;
    private static boolean shouldUpdate=false;
    @Override public int getAnimationGravityIn(){return Gravity.END;}
    @Override public int getAnimationGravityOut(){return Gravity.START;}
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toolbar=findViewById(R.id.toolbar);
        initActionBar(toolbar);
        toolbar.setTitle(R.string.settings);

        String name=getIntent().getStringExtra(KEY);
        type=SettingsFragment.Type.valueOf(name!=null?name:SettingsFragment.Type.List.name());
        switch (type){
            case List -> ((RecyclerView)findViewById(R.id.rv_list)).setAdapter(new ListAdapter(this));
            case Shelf -> replace(new SettingsShelf());
            default -> replace(new SettingsFragment(type));
        }
    }
    public void replace(Fragment fragment){
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, fragment)
                .commit();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if(type==SettingsFragment.Type.List && MainActivity.shouldUpdate && shouldUpdate){
            shouldUpdate=false;
            restart();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(type==SettingsFragment.Type.Shelf){sendBroadcast(new Intent(Constants.action_Update_Shelf));}
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        public enum Type{
            List(-1),
            Shelf(-1),
            Reader(R.xml.preferences_reader),
            General(R.xml.preferences_general);
            private final int xml_id;
            Type(int xml_id){
                this.xml_id=xml_id;
            }
            public int getXml_id(){return xml_id;}
        }
        Type type;
        public SettingsFragment(Type type){
            this.type=type;
        }
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(type.getXml_id(), rootKey);
            if(type==Type.General){
                setChangeListener(Constants.saved_path,(preference, newValue) -> {
                    BookService.init(newValue.toString());
                    MainActivity.shouldUpdate=true;
                    return false;
                });
                setChangeListener("THEME",(preference, newValue) -> {
                    MainActivity.shouldUpdate=true;
                    shouldUpdate=true;
                    ((SettingsActivity)requireActivity()).restart();
                    return false;
                });
                setChangeListener("language",(preference, newValue) -> {
                    MainActivity.shouldUpdate=true;
                    shouldUpdate=true;
                    setLocale((String)newValue,preference.getContext());
                    ((SettingsActivity)requireActivity()).restart();
                    return false;
                });
                setChangeListener("Timeout",(preference, newValue) -> {
                    if(newValue instanceof Number number && number.intValue()>0){
                        NetworkUtils.setNewTimeout(number.intValue()*1000L); return true;
                    }else{
                        return false;
                    }
                });
            }
            if(type==Type.Reader){
                setChangeListener(Constants.adjust_brightness_value,(preference, newValue) -> {
                    preference.setTitle(preference.getContext().getString(R.string.adjust_brightness_value)+": "+(newValue!=null ? newValue:0)+"%");
                    return true;
                });

                Preference translator=findPreference(Constants.use_another_translator);
                if(translator!=null){
                    boolean exists=!Utils.Translator.getTextTranslators(getContext(), resolveInfo -> resolveInfo).isEmpty();
                    translator.setOnPreferenceClickListener(
                            preference -> {
                                if(!exists && preference instanceof TwoStatePreference sp){
                                    sp.setChecked(!sp.isChecked());
                                    preference.getContext().startActivity(Utils.App.getIntentOfCallPlayMarketToInstall("ru.yandex.translate"));
                                    return false;
                                }else{
                                    return true;
                                }
                            });
                    translator.setSummaryProvider(preference -> exists ? "Yandex Translator":"Install Yandex Translator");
                }
            }
        }

        public void setChangeListener(String key,Preference.OnPreferenceChangeListener listener){
            Preference p=findPreference(key);
            if(p!=null){
                p.setOnPreferenceChangeListener(listener);
            }
        }

    }

    public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ListItem> {
        public class Container{
            public final String name;
            public final Intent intent;
            public final int icon_id;
            public Container(String name, int icon_id,Intent intent){
                this.name=name; this.icon_id=icon_id; this.intent=intent;
            }
        }
        ArrayList<Container> settings;
        public ListAdapter(Context context){
            Resources res=context.getResources();
            settings=new ArrayList<>();
            settings.add(new Container(res.getString(R.string.general),R.drawable.ic_tune,new Intent(context,SettingsActivity.class).putExtra(KEY,TYPE_GENERAL)));
            settings.add(new Container(res.getString(R.string.reader_options),R.drawable.ic_chrome_reader_mode_24dp,new Intent(context,SettingsActivity.class).putExtra(KEY,TYPE_READER)));
            settings.add(new Container(res.getString(R.string.action_settings_shelf),R.drawable.ic_shelf,new Intent(context,SettingsActivity.class).putExtra(SettingsActivity.KEY,SettingsActivity.TYPE_SHELF)));
            if(Logs.getDir()==null){Logs.init(SettingsActivity.this);}
            if(!Logs.getLogs().isEmpty()){
                settings.add(new Container(res.getString(R.string.logs),R.drawable.ic_code_24dp,new Intent(context, LogsActivity.class)));
            }
        }
        @NonNull
        @NotNull
        @Override
        public ListItem onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            return new ListItem(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_listitemview,parent,false));
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull ListItem holder, int position) {
            holder.bind(settings.get(position));
        }

        @Override
        public int getItemCount(){return settings.size();}

        public class ListItem extends RecyclerView.ViewHolder {
            ListItemView item;
            Intent intent;
            public ListItem(@NonNull @NotNull View itemView) {
                super(itemView);
                item=(ListItemView) itemView;
                item.setOnClickListener(v -> startActivity(intent,Gravity.START,Gravity.END));
            }
            public void bind(Container container){
                intent=container.intent;
                item.setIconResId(container.icon_id);
                item.setTitle(container.name);
            }
        }
    }
}