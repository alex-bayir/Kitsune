package org.alex.kitsune.ui.preview;

import android.content.Context;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;
import org.alex.kitsune.manga.Manga;
import org.jetbrains.annotations.NotNull;
import org.alex.kitsune.R;

public class PreviewPagerAdapter extends RecyclerView.Adapter<PreviewHolder> {


    private static final int[] TAB_TITLES_IDS = new int[]{R.string.DESCRIPTION, R.string.CHAPTERS,R.string.BOOKMARKS};
    public final String[] TAB_TITLES=new String[TAB_TITLES_IDS.length];
    private final Manga manga;
    private PreviewPage previewPage;
    private ChaptersPage chaptersPage;
    private BookMarksPage bookMarksPage;
    public PreviewPagerAdapter(Context context, Manga manga) {
        this.manga=manga;
        for(int i=0;i<TAB_TITLES.length;i++){
            TAB_TITLES[i]=context.getString(TAB_TITLES_IDS[i]);
        }
    }

    public String getTitle(int position){return TAB_TITLES[position];}

    @NonNull
    @NotNull
    @Override
    public PreviewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        switch (viewType){
            default: return previewPage=new PreviewPage(parent);
            case 1: return chaptersPage=new ChaptersPage(parent,manga);
            case 2: return bookMarksPage=new BookMarksPage(parent,manga);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull PreviewHolder holder, int position){holder.bind(manga);}

    @Override
    public int getItemCount(){return TAB_TITLES_IDS.length;}

    @Override
    public int getItemViewType(int position){return position;}

    public ChaptersPage getChaptersPage(){return chaptersPage;}

    public BookMarksPage getBookMarksPage(){return bookMarksPage;}

    public void bindPages(){
        bindPages(manga);
    }
    public void bindPages(Object obj){
        if(previewPage!=null){previewPage.bind(obj);}
        if(chaptersPage!=null){chaptersPage.bind(obj);}
        if(bookMarksPage!=null){bookMarksPage.bind(obj);}
    }
}