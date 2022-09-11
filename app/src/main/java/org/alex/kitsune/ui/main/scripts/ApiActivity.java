package org.alex.kitsune.ui.main.scripts;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import org.alex.kitsune.R;
import org.alex.kitsune.utils.Utils;

public class ApiActivity extends AppCompatActivity {
    Toolbar toolbar;
    TextView text;
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        setTheme(Utils.Theme.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api);
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        toolbar.setTitle("API");
        text=findViewById(R.id.text);
        text.setHorizontallyScrolling(true);
        text.setText(getText());
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); break;
        }
        return super.onOptionsItemSelected(item);
    }
    private String getText(){
        return "API для скриптов (примеры скриптов смотрите в разделе редактирования):\n" +
                "(написано криво (я самоучка и читать не люблю, да и вряд-ли вы напишите скрипт) так что ориентируйтесь на примеры)\n"+
                "(Если возникнут трудности можете писать в группу в вк в беседу скриптов и API)\n" +
                "(так же принимаются предложения о том что добавить или как улучшить приложение)\n"+
                "Первым делом вам необходимо определить глобальные переменные:\n" +
                "version - версия скрипта\n" +
                "provider – провайдер (например mangalib.me)\n" +
                "providerName – имя провайдера (например MangaLib)\n" +
                "sourceDescription – описание ресурса\n" +
                "\n" +
                "Дальше необходимо реализовать функцию query, чтобы осуществлять поиск манги и получать по ним основные сведения такие как ссылка на основную страницу данной манги(для функции update), название, рейтинг, описание и т.д. Все эти данные запихиваете в объект Wrapper и возвращаете список таких объектов.\n" +
                "ArrayList<Wrapper> query(String name, int page, Options[] params) //params нужен для расширенного поиска\n" +
                "\n" +
                "Дальше необходимо реализовать функцию update, чтобы обновлять данные по манге (точно так же как и в функции query так же получаете и запихиваете данные в объект Wrapper, только теперь ещё и основные данные по главам и вернуть нужно только один такой объект), а также получать данные о похожей манге (если есть возможность, не загружая другую страницу, а для таких случаев можно реализовать другую функцию которая указана ниже).\n" +
                "Wrapper update(String url)\n" +
                "\n" +
                "Дальше необходимо реализовать функцию getPages, чтобы получать ссылки на страницы (изображения) главы, вернуть нужно список объектов Page.\n" +
                "ArrayList<Page> getPages(Chapter chapter)\n" +
                "\n" +
                "Для расширенного поиска нужно реализовать функцию createAdvancedSearchOptions, которая будет возвращать  Options.\n" +
                "ArrayList<Options> createAdvancedSearchOptions()\n" +
                "\n" +
                "Для загрузки данных похожей манги можете реализовать функцию loadSimilar, которая будет возвращать ArrayList<Wrapper> (если в функции update не возможно получить эти данные не загружая другую страницу).\n" +
                "ArrayList<Wrapper> loadSimilar(Wrapper)\n" +
                "\n" +
                "Конструкторы:\n" +
                "public Wrapper(String url, int id, String name, String name_rus, String author, String genres, float rating, String status, String description, String thumbnail, ArrayList<Chapter> chapters, ArrayList<Wrapper> similars)\n" +
                "public Wrapper(String url, int id, String name, String name_rus, String author, String genres, float rating, int status, String description, String thumbnail_url, ArrayList<Chapter> chapters, ArrayList<Wrapper> similars) //chapters и similars можут быть null впрочем и остальные не важные тоже могут быть null\n" +
                "public Chapter(int id,int vol,float num,String name,long date) \n" +
                "public Chapter(int id,int vol,float num,String name,long date, ArrayList<Page> pages) \n" +
                "public Page(int id,float num,String url)\n" +
                "public Options(String string, Map<String,String> values, int mode) \n" +
                "public Options(String string,String descending,String ascending, @NotNull Map<String,String> values, int mode)\n" +
                "//mode=0 – выбор одного из группы\n" +
                "//mode=1 – два состояния (выбрано/не выбрано)\n" +
                "//mode=2 – три состояния (выбрано/исключено/не выбрано)\n" +
                "// descending и ascending – параметры для url\n" +
                "Методы:\n" +
                "Класса Wrapper\n" +
                "public static String loadPage(String url) //загружает страницу по адресу(нужен только в методе query)\n" +
                "public static Document loadDocument(String url) //тоже самое только в виде объекта Document\n" +
                "public static long parseDate(String date,String format)//парсинг даты\n" +
                "//остальные методы просто предотвращают NullPointerException\n" +
                "public static String attr(Element element, String attr, String defValue)\n" +
                "public static String attr(Element element, String attr)\n" +
                "public static String text(Element element, String defText)\n" +
                "public static String text(Element element)\n" +
                "public static String attr(Elements elements, String attr, String defValue)\n" +
                "public static String attr(Elements elements, String attr)\n" +
                "public static String text(Elements elements,String defText)\n" +
                "public static String text(Elements elements)\n" +
                "Класса Options \n" +
                "public String getTitleSortSelected() //возвращает descending или ascending\n" +
                "public String[] getSelected()\n" +
                "public String[] getDeselected()\n" +
                "public String[] getUnselected\n" +
                "Класса Сhapter\n" +
                "public int getId();\n" +
                "public int getVol();\n" +
                "public float getNum;\n" +
                "public String getName;\n" +
                "public long getDate;\n" +
                "\n" +
                "Полные имена классов:\n" +
                "org.alex.kitsune.manga.search.Options\n" +
                "org.alex.kitsune.manga.Wrapper\n" +
                "org.alex.kitsune.manga.Chapter\n" +
                "org.alex.kitsune.manga.Page\n" +
                "\n" +
                "org.jsoup:jsoup:1.14.3 \n" +
                "\torg.jsoup.nodes.Document\n" +
                "\torg.jsoup.nodes. Element\n" +
                "\tи другие (смотри библиотеку jsoup)\n";
    }
}
