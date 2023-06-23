package org.alex.kitsune.ui.shelf;

import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.commons.DiffCallback;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.book.views.BookHolder;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.ui.main.MainActivity;
import org.alex.kitsune.services.BookService;
import org.alex.kitsune.R;
import org.alex.kitsune.ui.preview.PreviewActivity;
import org.alex.kitsune.ui.reader.ReaderActivity;
import org.alex.kitsune.ui.settings.SettingsShelf;
import org.alex.kitsune.ui.shelf.favorite.FavoritesActivity;
import org.alex.kitsune.ui.shelf.history.HistoryActivity;
import org.alex.kitsune.ui.shelf.saved.SavedActivity;
import org.alex.kitsune.utils.NetworkUtils;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.alex.kitsune.Activity.animation;


public class Shelf extends Fragment implements MenuProvider {

    public static final String History=Constants.history,Saved=Constants.saved;
    LinkedHashMap<String, SettingsShelf.Container> shelf_sequence=new LinkedHashMap<>();
    SharedPreferences prefs;
    public SmoothProgressBar progress;
    private int p=0;
    RecyclerView root;
    Adapter adapter;
    MainActivity mainActivity;
    private final LinkedList<Runnable> tasks=new LinkedList<>();
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        requireActivity().removeMenuProvider(this);
        requireActivity().addMenuProvider(this);
        if(root==null){root=(RecyclerView) inflater.inflate(R.layout.content_shelf,container,false); mainActivity=(MainActivity)getActivity();}else{return root;}
        progress=mainActivity.findViewById(R.id.progress);
        progress.setIndeterminateDrawable(new SmoothProgressDrawable.Builder(progress.getContext()).colors(new int[]{0xff0000ff,0xff00ffff,0xff00ff00,0xffffff00,0xffff0000,0xffff00ff}).interpolator(new AccelerateDecelerateInterpolator()).callbacks(new SmoothProgressDrawable.Callbacks() {
            @Override public void onStop(){progress.setVisibility(View.GONE);}
            @Override public void onStart(){progress.setVisibility(View.VISIBLE);}
        }).build());
        progress.progressiveStop();
        prefs=PreferenceManager.getDefaultSharedPreferences(requireContext());
        adapter=new Adapter(getContext(),createWrappers(true)).initRV(root);
        IntentFilter filter=new IntentFilter(Constants.action_Update);
        filter.addAction(Constants.action_Update_Shelf);
        requireContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                adapter.update(createWrappers(true));
            }
        },filter);
        return root;
    }

    @Override
    public void onCreateMenu(@NonNull @NotNull Menu menu, @NonNull @NotNull MenuInflater menuInflater) {}

    @Override
    public boolean onMenuItemSelected(@NonNull @NotNull MenuItem menuItem) {
        switch(menuItem.getItemId()){
            case (R.id.check_for_updates): check_for_updates(); return true;
        }
        return false;
    }

    @Override
    public void onPrepareMenu(@NonNull @NotNull Menu menu) {
        menu.findItem(R.id.action_find_book).setVisible(true);
        menu.findItem(R.id.check_for_updates).setVisible(!BookService.isAllUpdated()).setEnabled(!BookService.isUpdating);
        menu.findItem(R.id.action_add_source).setVisible(false);
        menu.findItem(R.id.full).setVisible(false);
        menu.findItem(R.id.action_update_sctips).setVisible(false);
    }


    public void check_for_updates(){
        if(NetworkUtils.isNetworkAvailable(progress.getContext())){
            p=0;
            //progress.setSmoothProgressDrawableColor(0);
            progress.progressiveStart();
            final int size=BookService.getMap(BookService.Type.All).values().size();
            //ProgressDrawable pr=new ProgressDrawable().setMax(size);
            if(getActivity()!=null){getActivity().invalidateOptionsMenu();}
            BookService.isUpdating=true;
            for(Book book : BookService.getMap(BookService.Type.All).values()){
                if(!book.isUpdated()){
                    book.update((updated)->{
                        if(updated){
                            BookService.setCacheDirIfNull(book.getSimilar());
                            adapter.update(book);
                            if(book.getNotCheckedNew()>0){progress.getContext().sendBroadcast(new Intent(Constants.action_Update_New).putExtra(Constants.hash, book.hashCode()));}
                            if(++p==size){progress.progressiveStop(); BookService.isUpdating=false; mainActivity.setNew(BookService.getWithNew().size()); mainActivity.invalidateOptionsMenu();}
                            //pr.setProgress(p).setOnView(progress);
                        }
                    },throwable->{
                        if(++p==size){progress.progressiveStop(); BookService.isUpdating=false; mainActivity.setNew(BookService.getWithNew().size()); mainActivity.invalidateOptionsMenu();}
                        //pr.setProgress(p).setOnView(progress);
                    });
                }else{++p;}
            }
        }else{
            progress.progressiveStop();
            Toast.makeText(progress.getContext(), R.string.no_internet,Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        while(!tasks.isEmpty()){
            tasks.removeFirst().run();
        }
        if(prefs.getBoolean(Constants.update_on_start,true) && !BookService.isAllUpdated()){
            check_for_updates();
        }
        if(getActivity()!=null){
            getActivity().invalidateOptionsMenu();
            progress.setVisibility(BookService.isUpdating?View.VISIBLE:View.GONE);
        }
        new Handler(Looper.myLooper()).postDelayed(()->{if(adapter!=null){adapter.setEnableUpdate(true);}},100);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(adapter!=null){
            adapter.setEnableUpdate(false);
        }
    }

    private Wrapper createWrapper(String key, SettingsShelf.Container value){
        return (switch (key){
            case History-> new Wrapper(
                    key,
                    v->v.getContext().startActivity(new Intent(v.getContext(), HistoryActivity.class), animation(requireActivity(),Gravity.START,Gravity.END)),
                    BookService.getSorted(BookService.Type.History),4,value.count,value.first
            );
            case Saved  -> new Wrapper(
                    key,
                    v->v.getContext().startActivity(new Intent(v.getContext(), SavedActivity.class), animation(requireActivity(),Gravity.TOP,Gravity.START)),
                    BookService.getSorted(BookService.Type.Saved),4,value.count,value.first
            );
            default     -> new Wrapper(
                    key,
                    v->v.getContext().startActivity(new Intent(v.getContext(), FavoritesActivity.class).putExtra(Constants.category,key), animation(requireActivity(),Gravity.START,Gravity.END)),
                    BookService.getFavorites(key),3,value.count,value.first
            );
        }).init(
                book -> startActivity(new Intent(getContext(), PreviewActivity.class).putExtra(Constants.hash,book.hashCode()), animation(requireActivity(),Gravity.START,Gravity.END)),
                book -> startActivity(new Intent(getContext(), ReaderActivity.class).putExtra(Constants.hash,book.hashCode()).putExtra(Constants.history,true), animation(requireActivity(),Gravity.BOTTOM,Gravity.TOP))
        );
    }

    private List<Wrapper> createWrappers(boolean changed_sequence){
        shelf_sequence=shelf_sequence==null || changed_sequence ? SettingsShelf.getShelfSequence(prefs) : shelf_sequence;
        return createWrappers(shelf_sequence);
    }
    private List<Wrapper> createWrappers(LinkedHashMap<String, SettingsShelf.Container> shelf_sequence){
        List<Wrapper> list=new LinkedList<>();
        for(Map.Entry<String,SettingsShelf.Container> entry:shelf_sequence.entrySet()){
            list.add(createWrapper(entry.getKey(),entry.getValue()));
        }
        return list;
    }

    static class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
        List<Wrapper> all;
        List<Object> objects=new ArrayList<>(100);
        List<Integer> spans=new ArrayList<>(100);
        GridLayoutManager grid;
        private boolean enableUpdate=true;
        private List<Object> old;
        public Adapter(Context context,List<Wrapper> wrappers){
            grid=new GridLayoutManager(context,12);
            grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return spans.get(position);
                }
            });
            update(all=wrappers);
        }
        public void setEnableUpdate(boolean enableUpdate){
            if(this.enableUpdate!=enableUpdate){
                this.enableUpdate=enableUpdate;
                update(old,all);
                old=enableUpdate?null:objects;
            }
        }
        public boolean isEnableUpdate(){
            return enableUpdate;
        }
        public Adapter initRV(RecyclerView rv){
            rv.setAdapter(this);
            rv.setHasFixedSize(true);
            rv.setLayoutManager(getLayoutManager());
            return this;
        }
        public GridLayoutManager getLayoutManager(){return grid;}
        public void update(){
            update(all);
        }
        public void update(List<Wrapper> wrappers){
            if(enableUpdate){
                update(objects,wrappers);
            }else{
                update(null,wrappers);
            }
        }
        public void update(List<Object> old,List<Wrapper> wrappers){
            all=wrappers;
            objects=wrappers!=null?convert(wrappers):new ArrayList<>();
            calculateSpans();
            if(old!=null){
                new DiffCallback<>(old,objects).notifyUpdate(this,true);
            }
        }

        private void calculateSpans(){
            spans.clear();
            Wrapper wrap=null; Object last=null;
            for(Object obj:objects){
                if(obj instanceof Wrapper w){
                    wrap=w; spans.add(grid.getSpanCount());
                }else{
                    boolean all_width=wrap==null || wrap.mixed && last==wrap;
                    spans.add(grid.getSpanCount()/(all_width?1:wrap.columns));
                }
                last=obj;
            }
        }
        public List<Object> convert(List<Wrapper> wrappers){
            return wrappers.stream().flatMap(
                    w-> Math.min(w.list.size(),w.max)==0 ?
                            Stream.empty() : Stream.concat(Stream.of(w),w.list.stream().limit(w.max))
            ).collect(Collectors.toList());
        }
        public List<Book> getList(String key){
            return all.stream().filter(wrap->wrap.title.equals(key)).map(wrap->wrap.list).findFirst().orElse(null);
        }
        public void update(String key, Callback<List<Book>> callback){
            List<Book> list=getList(key);
            if(list!=null){
                callback.call(list);
                update();
            }
        }
        public void update(Book book){
            if(book !=null){
                for(int i=0;i<objects.size();i++){
                    if(objects.get(i).equals(book)){
                        notifyItemChanged(i);
                    }
                }
            }
        }
        private Wrapper getWrapper(int position){
            int save_pos=position;
            while (position>=0){
                if(objects.get(position--) instanceof Wrapper wrap){
                    return wrap;
                }
            }
            throw new IllegalArgumentException("Cannot find Wrapper before position:"+save_pos);
        }

        @Override
        public int getItemViewType(int position) {
            Object obj=objects.get(position);
            boolean full=obj instanceof Book && objects.get(position-1) instanceof Wrapper wrap && wrap.mixed;
            return obj instanceof Wrapper ? 0 : (full?1:2);
        }

        @NonNull
        @NotNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            return switch (viewType){
                case 0->new TitleHolder(parent);
                case 2->new BookHolder(parent,null,null,false,false);
                case 1->new BookHolder(parent,null,null,true,false);
                default -> throw new IllegalArgumentException("ViewHolder can not be null");
            };
        }
        @Override
        public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position) {
            Object obj=objects.get(position);
            if(holder instanceof TitleHolder th && obj instanceof Wrapper wrap){
                th.bind(wrap);
            }else if(holder instanceof BookHolder mh && obj instanceof Book book){
                Wrapper wrap=getWrapper(position);
                mh.setOnClickListeners(
                        wrap.item!=null? (v, p) -> wrap.item.call(book):null,
                        wrap.button!=null? (v, p)-> wrap.button.call(book):null
                );
                mh.bind(book,false,true,0);
            }
        }

        @Override
        public int getItemCount() {
            return objects.size();
        }

        static class TitleHolder extends RecyclerView.ViewHolder{
            TextView title;
            Button more;
            public TitleHolder(ViewGroup parent) {
                super(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shelf_title, parent, false));
                title=itemView.findViewById(R.id.title);
                more=itemView.findViewById(R.id.more);
            }
            public void bind(Wrapper wrap){
                title.setText(wrap.title);
                more.setOnClickListener(wrap.more);
            }
        }
    }
    static class Wrapper{
        String title;
        View.OnClickListener more;
        List<Book> list;
        int max;
        int columns;
        boolean mixed;
        Callback<Book> item;
        Callback<Book> button;

        public Wrapper(String title, View.OnClickListener more, List<Book> list, int columns, int max_rows, boolean first_full){
            this.title=title;
            this.more = more;
            this.list=list;
            this.mixed=first_full;
            this.columns=columns;
            max=mixed ? columns*(max_rows-1)+1 : columns*max_rows;
        }
        public Wrapper init(Callback<Book> item, Callback<Book> item_button){
            this.item=item;
            this.button=item_button;
            return this;
        }
        @Override
        public boolean equals(@Nullable @org.jetbrains.annotations.Nullable Object obj) {
            return obj instanceof Wrapper wrap && Objects.equals(this.title,wrap.title);
        }
    }
}