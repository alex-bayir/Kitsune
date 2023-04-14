package org.alex.kitsune.ui.search;

import android.content.Intent;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;
import org.alex.kitsune.R;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.book.views.BookAdapter;
import org.alex.kitsune.commons.Callback;
import org.alex.kitsune.commons.Callback2;
import org.alex.kitsune.services.BookService;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.ui.preview.PreviewActivity;
import org.alex.kitsune.utils.Binder;
import org.alex.kitsune.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class SourceSearchAdapter extends RecyclerView.Adapter<SourceSearchAdapter.SourceSearchHolder>{
    final List<String> sources;
    final TreeMap<String,List<Book>> books=new TreeMap<>();
    final Callback<Integer>[] updates;
    final boolean intercept_scrolling;
    Callback2<String, List<Book>> callback;
    public SourceSearchAdapter(List<String> sources,boolean intercept_scrolling){
        this.sources=sources;
        this.intercept_scrolling=intercept_scrolling;
        callback=(source,list)->{
            if(list!=null){
                BookService.setCacheDirIfNull(list);
                books.put(source,list);
                notifyItemChanged(sources.indexOf(source));
            }
        };
        updates=new Callback[sources.size()];
    }
    public void clear(){
        books.clear();
        notifyItemRangeChanged(0,getItemCount());
    }
    public void update(int hash){
        for(Callback<Integer> callback:updates){
            if(callback!=null){callback.call(hash);}
        }
    }
    public Callback2<String, List<Book>> getCallback() {
        return callback;
    }
    public List<String> getSources(){
        return sources;
    }
    public List<Book> getBooks(String source){
        return books.get(source);
    }
    @NonNull
    @Override
    public SourceSearchHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SourceSearchHolder(parent,intercept_scrolling);
    }

    @Override
    public void onBindViewHolder(@NonNull SourceSearchHolder holder, int position) {
        String source=sources.get(position);
        holder.bind(source,books.getOrDefault(source,new ArrayList<>(0)));
        updates[position]=holder.getUpdateCallback();
    }

    @Override
    public int getItemCount() {
        return sources.size();
    }

    public static class SourceSearchHolder extends RecyclerView.ViewHolder{
        TextView title;
        View more;
        RecyclerView rv;
        BookAdapter adapter;
        SmoothProgressBar progress;
        public SourceSearchHolder(ViewGroup parent, boolean intercept_scrolling) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_source_search,parent,false));
            title=itemView.findViewById(R.id.title);
            more=itemView.findViewById(R.id.more);
            rv=itemView.findViewById(R.id.rv_list);
            progress=itemView.findViewById(R.id.progress);

            adapter=new BookAdapter(new ArrayList<>(0), BookAdapter.Mode.GRID,book->{
                adapter.add(BookService.getOrPutNewWithDir(book));
                itemView.getContext().startActivity(new Intent(itemView.getContext(), PreviewActivity.class).putExtra(Constants.hash,book.hashCode()));
            });
            adapter.initRV(rv,1,RecyclerView.HORIZONTAL,false);
            rv.setHorizontalScrollBarEnabled(true);
            more.setOnClickListener(v->{
                v.getContext().startActivity(
                        new Intent(v.getContext(), SearchResultsActivity.class)
                                .putExtra("source",title.getText())
                                .putExtras(new Binder<>(adapter.getList()).toBundle("books"))
                );
            });
            if(intercept_scrolling){
                Utils.setHorizontalInterceptorDisallow(rv,v->itemView.getParent());
            }
            progress.setIndeterminateDrawable(new SmoothProgressDrawable.Builder(progress.getContext()).colors(new int[]{0xff0000ff,0xff00ffff,0xff00ff00,0xffffff00,0xffff0000,0xffff00ff}).interpolator(new AccelerateDecelerateInterpolator()).mirrorMode(true).callbacks(new SmoothProgressDrawable.Callbacks() {
                @Override public void onStop(){progress.setVisibility(View.GONE);}
                @Override public void onStart(){progress.setVisibility(View.VISIBLE);}
            }).build());
            progress.progressiveStop();
        }
        public Callback<Integer> getUpdateCallback(){
            return hash-> adapter.update(BookService.get(hash));
        }
        public void bind(String source,List<Book> books){
            title.setText(source);
            adapter.replace(books);
            if(books.size()==0){
                progress.progressiveStart();
            }else{
                progress.progressiveStop();
            }
            more.setEnabled(books.size()>0);
        }
    }
}
