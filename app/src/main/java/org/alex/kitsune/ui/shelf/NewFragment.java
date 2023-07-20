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
import org.alex.kitsune.book.Book;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.ui.main.MainActivity;
import org.alex.kitsune.services.BookService;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.SwipeRemoveHelper;
import org.alex.kitsune.book.views.BookAdapter;
import org.alex.kitsune.ui.preview.PreviewActivity;
import org.alex.kitsune.ui.reader.ReaderActivity;
import org.jetbrains.annotations.NotNull;

import static org.alex.kitsune.Activity.animation;

public class NewFragment extends Fragment implements MenuProvider {
    RecyclerView rv;
    BookAdapter adapter;
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
        prefs=PreferenceManager.getDefaultSharedPreferences(requireContext());
        adapter=new BookAdapter(null, BookAdapter.Mode.LIST,
                book -> startActivity(new Intent(getContext(), PreviewActivity.class).putExtra(Constants.hash,book.hashCode()),animation(requireActivity(),Gravity.START,Gravity.END)),
                book -> startActivity(new Intent(getContext(), ReaderActivity.class).putExtra(Constants.hash,book.hashCode()).putExtra(Constants.history,true),animation(requireActivity(),Gravity.TOP,Gravity.BOTTOM))
        );
        adapter.setShowSource(false);
        adapter.setShowCheckedNew(false);
        adapter.initRV(rv,1);
        mainActivity=(MainActivity)getActivity();
        IntentFilter filter=new IntentFilter(Constants.action_Update_New);
        filter.addAction(Constants.action_Update);
        requireContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Book book=BookService.get(intent.getIntExtra(Constants.hash,-1));
                if(book!=null && adapter!=null){
                    if(Constants.action_Update_New.equals(intent.getAction())){
                        adapter.add(book);
                        mainActivity.setNew(adapter.getItemCount());
                    }else if(Constants.action_Update.equals(intent.getAction())){
                        adapter.update(book);
                    }
                }
            }
        }, filter);
        bind();
        SwipeRemoveHelper.setup(rv,new SwipeRemoveHelper(requireContext(),R.color.checked,R.drawable.ic_done_all_white,24, i -> {
            Book book=adapter.remove(i);
            book.checkedNew();
            mainActivity.setNew(adapter.getItemCount());
            requireContext().sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash, book.hashCode()));
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
        adapter.replace(BookService.getWithNew());
        mainActivity.setNew(adapter.getItemCount());
    }

    @Override
    public void onResume() {
        super.onResume();
        bind();
        if(adapter!=null){
            adapter.setEnableUpdate(true);
        }
        if(getActivity()!=null){getActivity().invalidateOptionsMenu();}
    }

    @Override
    public void onPause() {
        super.onPause();
        if(adapter!=null){
            adapter.setEnableUpdate(false);
        }
    }

    @Override
    public void onPrepareMenu(@NonNull @NotNull Menu menu) {
        menu.findItem(R.id.action_find_book).setVisible(true);
        menu.findItem(R.id.check_for_updates).setVisible(!BookService.isAllUpdated()).setEnabled(!BookService.isUpdating);
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
