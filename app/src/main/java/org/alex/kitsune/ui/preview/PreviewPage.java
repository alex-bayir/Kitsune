package org.alex.kitsune.ui.preview;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import com.alex.listitemview.ListItemView;
import org.alex.kitsune.commons.*;
import org.alex.kitsune.manga.views.MangaAdapter;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.services.MangaService;
import org.alex.kitsune.R;
import com.alex.ratingbar.RatingBar;
import org.alex.kitsune.manga.Manga;
import org.alex.kitsune.ui.reader.ReaderActivity;
import org.alex.kitsune.ui.search.AdvancedSearchActivity;
import org.alex.kitsune.ui.settings.SettingsShelf;
import org.alex.kitsune.ui.shelf.Catalogs;
import org.alex.kitsune.ui.shelf.Shelf;
import org.alex.kitsune.utils.NetworkUtils;
import org.alex.kitsune.utils.Utils;
import org.jetbrains.annotations.NotNull;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.alex.kitsune.ui.preview.PreviewActivity.CALL_FILE_STORE;
import static org.alex.kitsune.ui.preview.PreviewActivity.PERMISSION_REQUEST_CODE;


public class PreviewPage extends PreviewHolder {
    private final ImageView cover;
    private final ImageView backdrop;
    private final TextView info;
    private final RatingBar ratingBar;
    private final Button read;
    private final AppCompatImageButton favorite, web;
    private final ListItemView genres;
    private final ListItemView averageTime;
    private final ListItemView description;
    private MangaAdapter similar;
    private final Drawable caution;
    private final WebViewBottomSheetDialog web_dialog=new WebViewBottomSheetDialog();


    public PreviewPage(ViewGroup parent){
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_preview_page,parent,false));
        backdrop=itemView.findViewById(R.id.backdrop);
        cover=itemView.findViewById(R.id.cover);
        cover.setOnClickListener(v -> new AlertDialog.Builder(v.getContext()).setView(new AspectRatioImageView(v.getContext(),cover.getScaleType(),cover.getDrawable())).create().show());
        cover.setOnLongClickListener(v->{Utils.Activity.callFilesStore((Activity)v.getContext(),CALL_FILE_STORE,"image/*",PERMISSION_REQUEST_CODE); return true;});
        caution=cover.getDrawable();
        info=itemView.findViewById(R.id.info);
        info.setMovementMethod(LinkMovementMethod.getInstance());
        favorite=itemView.findViewById(R.id.button_favourite);
        web=itemView.findViewById(R.id.button_web);
        web.setOnClickListener(v-> web_dialog.show(((FragmentActivity)web.getContext()).getSupportFragmentManager(),""));
        web.setOnLongClickListener(v -> {Utils.showToolTip(web,R.string.auth_help_info); return true;});
        read=itemView.findViewById(R.id.button_read);
        ratingBar=itemView.findViewById(R.id.ratingBar);
        genres=itemView.findViewById(R.id.genres);
        genres.changeSubtitleSingleLine().setOnClickListener(v -> genres.changeSubtitleSingleLine());
        genres.getSubtitleView().setMovementMethod(LinkMovementMethod.getInstance());
        genres.setOnLongClickListener(v->{Utils.setClipboard(itemView.getContext(), genres.getSubtitle()); Toast.makeText(itemView.getContext(),R.string.text_copied,Toast.LENGTH_SHORT).show(); return true;});
        averageTime=itemView.findViewById(R.id.average_read_time);
        description=itemView.findViewById(R.id.description);
        description.setOnClickListener(v-> description.changeSubtitleSingleLine());
        description.setOnLongClickListener(v-> {Utils.setClipboard(itemView.getContext(), description.getSubtitle()); Toast.makeText(itemView.getContext(),R.string.text_copied,Toast.LENGTH_SHORT).show(); return true;});
        RecyclerView rv=itemView.findViewById(R.id.rv_list);
        similar=new MangaAdapter(null, MangaAdapter.Mode.GRID, m -> {
            similar.add(MangaService.getOrPutNewWithDir(m));
            itemView.getContext().startActivity(new Intent(itemView.getContext(), PreviewActivity.class).putExtra(Constants.hash,m.hashCode()));
        });
        similar.initRV(rv,1,RecyclerView.HORIZONTAL,false);
        rv.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener(){@Override public boolean onInterceptTouchEvent(@NonNull @NotNull RecyclerView rv, @NonNull @NotNull MotionEvent e){itemView.getParent().requestDisallowInterceptTouchEvent(true);return false;}});
    }


    private static Dialog createDialog(Context context,Manga manga){
        View v1=LayoutInflater.from(context).inflate(R.layout.dialog_choose_category,null);
        Dialog chooseCategory=new AlertDialog.Builder(context).setView(v1).create();
        View v2=LayoutInflater.from(context).inflate(R.layout.dialog_input,null);
        Dialog inputNewCategory=new AlertDialog.Builder(context).setView(v2).create();


        RadioGroup group=v1.findViewById(R.id.group);
        v1.findViewById(R.id.close).setOnClickListener(v -> chooseCategory.cancel());
        v1.findViewById(R.id.create).setOnClickListener(v -> inputNewCategory.show());
        v1.findViewById(R.id.remove).setOnClickListener(v -> {group.clearCheck(); chooseCategory.cancel(); v.getContext().sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash,manga.hashCode()).putExtra(Constants.option,Constants.favorites));});

        setGroups(context, MangaService.getCategories(),group,manga!=null ? manga.getCategoryFavorite() : null);
        group.setOnCheckedChangeListener((group1, checkedId) -> {
            if(manga!=null){
                manga.setCategoryFavorite(checkedId>=0 ? MangaService.getCategories().toArray(new String[0])[checkedId] : null);
                MangaService.allocate(manga,false);
                context.sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash,manga.hashCode()).putExtra(Constants.option,Constants.favorites));
            }
            chooseCategory.cancel();
        });


        EditText input=v2.findViewById(R.id.input);
        v2.findViewById(R.id.close).setOnClickListener(v -> inputNewCategory.cancel());
        v2.findViewById(R.id.create).setOnClickListener(v -> {
            if(manga!=null && input.getText()!=null && input.getText().length()>0){
                String category=input.getText().toString();
                HashSet<String> categories=new HashSet<>(MangaService.getCategories());
                categories.add(Shelf.History);
                categories.add(Shelf.Saved);
                if(!categories.contains(category)){
                    MangaService.getCategories().add(category);
                    SettingsShelf.add(PreferenceManager.getDefaultSharedPreferences(v.getContext()),category);
                    setGroups(v.getContext(),MangaService.getCategories(),group,manga.getCategoryFavorite());
                    manga.setCategoryFavorite(input.getText().toString());
                    MangaService.allocate(manga,false);
                    v.getContext().sendBroadcast(new Intent(Constants.action_Update).putExtra(Constants.hash,manga.hashCode()).putExtra(Constants.option,Constants.favorites));
                    inputNewCategory.cancel();
                }else{
                    input.setError("This category name already exist");
                }
            }else{
                input.setError("Length must more than zero");
            }
        });
        return chooseCategory;
    }
    @Override
    public void bind(Manga manga){
        bind(manga,manga.isUpdated() || !NetworkUtils.isNetworkAvailable(itemView.getContext()));
    }
    public void bind(Throwable th){
        notifyError(th.getCause()!=null ? th.getCause() : th);
    }

    public void bind(Manga manga, boolean full){
        manga.loadThumbnail(drawable -> {
            cover.setImageDrawable(drawable==null ? caution : drawable);
            cover.setScaleType(drawable==null ? ImageView.ScaleType.CENTER : ImageView.ScaleType.CENTER_CROP);
            backdrop.setImageDrawable(drawable==null ? caution : drawable);
            backdrop.setScaleType(drawable==null ? ImageView.ScaleType.CENTER : ImageView.ScaleType.CENTER_CROP);
        });
        info.setText(createText(info.getContext(),manga,full));
        ratingBar.setRating(manga.getRating(),true);
        genres.setSubtitle(manga.getGenres((view, text)->view.getContext().startActivity(new Intent(view.getContext(),AdvancedSearchActivity.class).putExtra(Constants.catalog,manga.getSource()).putExtra(Constants.option,text!=null ? text.toString() : null))));

        averageTime.setSubtitle(full ? calculateTime(itemView.getResources(), manga.getChapters().size()) : averageTime.getResources().getString(R.string.calculating));
        description.setSubtitle(full ? (manga.isUpdated() || manga.getDescription()!=null ? manga.getDescription(Html.FROM_HTML_MODE_COMPACT,description.getResources().getString(R.string.no_description)) : description.getResources().getString(R.string.error_has_occurred)) : description.getResources().getString(R.string.loading));
        if(manga.getHistory()!=null && manga.getNumChapterHistory()>=0){read.setText(R.string.CONTINUE);}
        read.setOnClickListener(v -> v.getContext().startActivity(new Intent(v.getContext(),ReaderActivity.class).putExtra(Constants.hash,manga.hashCode()).putExtra(Constants.history,true)));
        read.setEnabled(manga.getChapters().size()>0);
        favorite.setOnClickListener(v -> createDialog(v.getContext(),manga).show());
        similar.replace(MangaService.replaceIfExists(manga.getSimilar(),MangaService.getAll()));
        final String source=manga.getSource(),url_web=manga.getUrl_WEB();
        web_dialog.setCallback(web->{
            web.setWebViewClient(new WebViewClient(){
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    web_dialog.setOnCloseListener(()->{
                        Catalogs.updateCookies(view.getContext(),source,CookieManager.getInstance().getCookie(url));
                        manga.update(update->{
                            if(itemView.getParent() instanceof View v && v.getContext() instanceof PreviewActivity pa){
                                pa.updateContent();
                            }
                        },null,true);
                    });
                }
            });
            web.loadUrl(url_web);
        });
    }

    private String calculateTime(Resources res, int size) {
        size*=323;
        int hours=size/3600, minutes=(size%3600)/60;
        return (res.getQuantityString(R.plurals.hour, hours, hours) + " " + res.getQuantityString(R.plurals.minute, minutes, minutes));
    }

    public static void setGroups(Context context, HashSet<String> groups, RadioGroup group, String category){
        if(groups!=null){
            group.clearCheck();
            group.removeAllViews();
            int i=0;
            for(String g:groups){
                RadioButton fb=new RadioButton(context);
                fb.setText(g);
                fb.setId(i++);
                group.addView(fb);
                if(g.equals(category)){group.check(i-1);}
            }
        }
    }
    public static CharSequence createText(Context context, Manga manga, boolean count_known){
        SpannableStringBuilder builder=new SpannableStringBuilder();
        builder.append(context.getString(R.string.Chapters), new StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(count_known ? manga.getChapters().size()+"" : "?");
        if(manga.getAuthor() instanceof Map<?,?> map){
            builder.append("\n");
            AtomicInteger count=new AtomicInteger();
            map.entrySet().stream().filter(entry-> entry.getKey() instanceof String && entry.getValue() instanceof String).forEach(entry->{
                if(count.getAndIncrement()>0){
                    builder.append(", ");
                }
                builder.append((String)entry.getKey(), new ClickSpan((String)entry.getValue(), (view, text)->view.getContext().startActivity(new Intent(view.getContext(),AdvancedSearchActivity.class).putExtra(Constants.catalog,manga.getSource()).putExtra(Constants.author,(String)entry.getKey()).putExtra(Constants.author_url,text!=null ? text.toString() : null))), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            });
        }else if(manga.getAuthor() instanceof String str){
            builder.append("\n");
            builder.append(str);
        }
        builder.append("\n");
        builder.append(context.getString(R.string.Source_),new StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(manga.getSource());
        builder.append("\n");
        builder.append(context.getString(R.string.Status_),new StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(manga.getStatus(context));
        return builder;
    }

    public void notifyError(Throwable th){
        description.setSubtitle("Type: "+th.getClass().getName()+"\nCause: "+th.getMessage()+"\nStackTrace see in menu");
    }
}
