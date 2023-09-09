package org.alex.kitsune.ui.shelf.favorite;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import org.alex.kitsune.Activity;
import com.google.android.material.tabs.TabLayoutMediator;
import org.alex.kitsune.commons.DiffCallback;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.book.views.BookAdapter;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.services.BookService;
import org.alex.kitsune.R;
import org.alex.kitsune.ui.preview.PreviewActivity;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FavoritesActivity extends Activity {

    Toolbar toolbar;
    ViewPager2 viewPager;
    CategoriesAdapter adapter;
    TabLayout tabs;
    ArrayList<String> categories;
    private final DiffCallback<String> notify=new DiffCallback<>();

    @Override public int getAnimationGravityIn(){return Gravity.END;}
    @Override public int getAnimationGravityOut(){return Gravity.START;}
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);
        toolbar=findViewById(R.id.toolbar);
        initActionBar(toolbar);
        toolbar.setTitle(R.string.Favorites);
        viewPager=findViewById(R.id.view_pager);

        categories=new ArrayList<>(BookService.getCategories());
        adapter=new CategoriesAdapter(categories);
        viewPager.setAdapter(adapter);
        tabs=findViewById(R.id.tabs);
        for(String category:categories){
            tabs.addTab(tabs.newTab().setText(category));
        }

        new TabLayoutMediator(tabs, viewPager, true, (tab, position) -> tab.setText(categories.get(position))).attach();
        viewPager.setCurrentItem(Math.max(categories.indexOf(getIntent().getStringExtra(Constants.category)), 0),false);
    }
    @Override
    protected void onResume() {
        super.onResume();
        categories.clear();
        categories.addAll(BookService.getCategories());
        adapter.setCategories(categories);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sorting_source, menu);
        menu.findItem(adapter.isShowSource() ? R.id.source : R.id.status).setChecked(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.latest) -> {item.setChecked(true);adapter.sort(Book.CategoryTimeComparator);}
            case (R.id.alphabetical) -> {item.setChecked(true);adapter.sort(Book.AlphabeticalComparator);}
            case (R.id.alphabetical_alt) -> {item.setChecked(true);adapter.sort(Book.AlphabeticalComparatorAlt);}
            case (R.id.rating) -> {item.setChecked(true);adapter.sort(Book.RatingComparator);}
            case (R.id.status), (R.id.source) -> {item.setChecked(true);adapter.setShowSource(item.getItemId() == R.id.source);}
        }
        return super.onOptionsItemSelected(item);
    }

    public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoryHolder>{
        List<String> categories=new ArrayList<>();
        List<BookAdapter> adapters=new ArrayList<>();
        boolean showSource=true;
        Comparator<Book> comparator=Book.CategoryTimeComparator;
        public CategoriesAdapter(List<String> categories){
            setCategories(categories);
        }
        public void setCategories(List<String> categories){
            notify.init(new ArrayList<>(this.categories),this.categories=categories,false).notifyUpdate(this);
        }
        public void setShowSource(boolean showSource){
            this.showSource=showSource;
            notifyItemRangeChanged(0, getItemCount());
        }
        public boolean isShowSource(){return showSource;}
        public void sort(Comparator<Book> comparator){
            this.comparator=comparator;
            for(BookAdapter adapter:adapters){
                adapter.sort(comparator);
            }
        }
        private BookAdapter add(BookAdapter adapter){adapters.add(adapter); return adapter;}
        @NonNull
        @NotNull
        @Override
        public CategoryHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            return new CategoryHolder(parent, add(new BookAdapter(null, BookAdapter.Mode.LIST, book -> startActivity(new Intent(FavoritesActivity.this, PreviewActivity.class).putExtra(Constants.hash, book.hashCode()),Gravity.START,Gravity.END))));
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull CategoryHolder holder, int position) {
            holder.bind(categories.get(position), showSource, comparator);
        }

        @Override
        public int getItemCount(){return categories.size();}

        public static class CategoryHolder extends RecyclerView.ViewHolder{
            final BookAdapter adapter;
            public CategoryHolder(@NonNull @NotNull ViewGroup parent, BookAdapter adapter) {
                super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_recyclerview_list, parent, false));
                this.adapter=adapter;
                this.adapter.initRV(itemView.findViewById(R.id.rv_list),1);
            }
            public void bind(String category, boolean showSource, Comparator<Book> comparator){
                adapter.setShowSource(showSource, false);
                adapter.replace(BookService.getFavorites(category), comparator, false);
            }
        }
    }

}