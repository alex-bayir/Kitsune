package org.alex.kitsune.ui.preview;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.*;
import org.alex.kitsune.commons.HolderListener;
import org.alex.kitsune.commons.HolderMenuItemClickListener;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.R;
import org.alex.kitsune.manga.BookMark;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.ui.reader.ReaderActivity;
import org.alex.kitsune.utils.Utils;

public class BookMarksPage extends PreviewHolder implements HolderListener, HolderMenuItemClickListener {
    RecyclerView rv;
    CustomAdapter<BookMark> adapter;
    TextView noItems;
    public BookMarksPage(ViewGroup parent){
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_recyclerview_list,parent,false));
        noItems=itemView.findViewById(R.id.text);
        rv=itemView.findViewById(R.id.rv_list);
        adapter=new CustomAdapter<>(R.layout.item_bookmark,this,this::onMenuItemClick,rv,null);
        //rv.addItemDecoration(new DividerItemDecoration(rv.getContext(), DividerItemDecoration.VERTICAL));
        rv.setLayoutManager(new GridLayoutManager(itemView.getContext(),2));
        noItems.setText(R.string.No_bookmarks);
        noItems.setVisibility(View.VISIBLE);
        Utils.registerAdapterDataChangeRunnable(adapter,()->noItems.setVisibility(adapter.getItemCount()==0 ? View.VISIBLE : View.GONE));
    }

    @Override
    public void bind(Manga manga) {
        adapter.setManga(manga);
    }

    @Override
    public void onItemClick(View v, int index) {
        itemView.getContext().startActivity(new Intent(itemView.getContext(), ReaderActivity.class).putExtra(Constants.hash,adapter.manga.hashCode()).putExtra(Constants.bookmark, index));
    }

    @Override
    public boolean onItemLongClick(View v, int index){return false;}

    @Override
    public boolean onMenuItemClick(int position, MenuItem item) {
        switch (item.getItemId()){
            case BookMarkHolder.REMOVE: adapter.manga.removeBookMark(adapter.getList().remove(position)); adapter.notifyItemRemoved(position); break;
        }
        return false;
    }
}
