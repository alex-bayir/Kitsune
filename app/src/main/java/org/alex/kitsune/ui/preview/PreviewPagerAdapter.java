package org.alex.kitsune.ui.preview;

import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;
import org.alex.kitsune.book.Book;
import org.jetbrains.annotations.NotNull;
import org.alex.kitsune.R;

public class PreviewPagerAdapter extends RecyclerView.Adapter<PreviewHolder> {
    private static final int[] TAB_TITLES_IDS = new int[]{R.string.DESCRIPTION, R.string.CHAPTERS,R.string.BOOKMARKS};
    private final Book book;
    private PreviewPage previewPage;
    private ChaptersPage chaptersPage;
    private BookMarksPage bookMarksPage;
    public PreviewPagerAdapter(Book book) {
        this.book=book;
    }
    public int getTitle(int position){return TAB_TITLES_IDS[position];}
    @NonNull
    @NotNull
    @Override
    public PreviewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        return switch (viewType) {
            default -> previewPage = previewPage!=null? previewPage : new PreviewPage(parent);
            case 1 -> chaptersPage = chaptersPage!=null? chaptersPage : new ChaptersPage(parent);
            case 2 -> bookMarksPage = bookMarksPage!=null? bookMarksPage : new BookMarksPage(parent);
        };
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull PreviewHolder holder, int position){holder.bind(book);}

    @Override
    public int getItemCount(){return TAB_TITLES_IDS.length;}

    @Override
    public int getItemViewType(int position){return position;}

    public ChaptersPage getChaptersPage(){return chaptersPage;}

    public BookMarksPage getBookMarksPage(){return bookMarksPage;}

    public void bindPages(){
        notifyItemRangeChanged(0,getItemCount());
    }
    public void bindPages(Throwable th){
        if(previewPage!=null){previewPage.bind(th);}
    }
}