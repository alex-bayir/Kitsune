package org.alex.kitsune.ui.shelf;

import android.content.*;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.ui.main.MainActivity;
import org.alex.kitsune.services.MangaService;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.SwipeRemoveHelper;
import org.alex.kitsune.manga.views.MangaAdapter;
import org.alex.kitsune.ui.preview.PreviewActivity;
import org.alex.kitsune.ui.reader.ReaderActivity;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class NewFragment extends Fragment implements MenuProvider {
    RecyclerView rv;
    MangaAdapter adapter;
    SharedPreferences prefs;
    View root;
    MainActivity mainActivity;
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        requireActivity().removeMenuProvider(this);
        requireActivity().addMenuProvider(this);
        if(root!=null){return root;}
        root=inflater.inflate(R.layout.fragment_recyclerview_list,container,false);
        rv=root.findViewById(R.id.rv_list);
        prefs=PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(getContext()));
        adapter=new MangaAdapter(null, MangaAdapter.Mode.LIST,
                manga -> startActivity(new Intent(getContext(), PreviewActivity.class).putExtra(Constants.hash,manga.hashCode())),
                manga -> startActivity(new Intent(getContext(), ReaderActivity.class).putExtra(Constants.hash,manga.hashCode()).putExtra(Constants.history,true))
        );
        adapter.setShowSource(false);
        adapter.setShowUpdated(true);
        adapter.initRV(rv,1);
        mainActivity=(MainActivity)getActivity();
        IntentFilter filter=new IntentFilter(Constants.action_Update_New);
        filter.addAction(Constants.action_Update);
        getContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Manga manga=MangaService.get(intent.getIntExtra(Constants.hash,-1));
                if(manga!=null && adapter!=null){
                    if(Constants.action_Update_New.equals(intent.getAction())){
                        adapter.add(manga);
                        mainActivity.setNew(adapter.getItemCount());
                    }else if(Constants.action_Update.equals(intent.getAction())){
                        String tmp=intent.getStringExtra(Constants.option);
                        switch(tmp!=null ? tmp : ""){
                            case Constants.load:
                            case Constants.delete: adapter.update(manga); break;
                        }
                    }
                }
            }
        }, filter);
        bind();
        SwipeRemoveHelper.setup(rv,new SwipeRemoveHelper(requireContext(),R.color.checked,R.drawable.ic_done_all_white,24, i -> {
            Manga manga=adapter.remove(i);
            manga.checkedNew();
            mainActivity.setNew(adapter.getItemCount());
            getContext().sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash,manga.hashCode()));
        }));
        requireContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                bind();
            }
        },new IntentFilter(Constants.action_Update_Shelf));
        return root;
    }

    public void bind(){
        adapter.replace(MangaService.getWithNew());
        mainActivity.setNew(adapter.getItemCount());
    }

    @Override
    public void onResume() {
        super.onResume();
        bind();
        if(getActivity()!=null){getActivity().invalidateOptionsMenu();}
    }

    @Override
    public void onPrepareMenu(@NonNull @NotNull Menu menu) {
        menu.findItem(R.id.action_find_manga).setVisible(true);
        menu.findItem(R.id.check_for_updates).setVisible(!MangaService.isAllUpdated()).setEnabled(!MangaService.isUpdating);
        menu.findItem(R.id.action_add_source).setVisible(false);
        menu.findItem(R.id.full).setVisible(false);
        menu.findItem(R.id.action_update_sctips).setVisible(false);
    }

    @Override
    public void onCreateMenu(@NonNull @NotNull Menu menu, @NonNull @NotNull MenuInflater menuInflater) {}

    @Override
    public boolean onMenuItemSelected(@NonNull @NotNull MenuItem menuItem) {
        return false;
    }
}
