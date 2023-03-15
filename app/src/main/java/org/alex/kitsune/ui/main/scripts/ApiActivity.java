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
                "version – версия скрипта\n" +
                "domain – провайдер (например mangalib.me)\n" +
                "source – имя провайдера (например MangaLib)\n" +
                "description – описание ресурса\n" +
                "auth_tokens – токены для авторизации\n"+
                "\n" +
                "Дальше необходимо реализовать функцию query, чтобы осуществлять поиск манги и получать по ним основные сведения такие как ссылка на основную страницу данной манги(для функции update), название, рейтинг, описание и т.д. Все эти данные запихиваете в ассоциативный массив и возвращаете список таких массивов.\n" +
                "List<Map<String,Object>> query(String name, int page, Options[] params) //params нужен для расширенного поиска\n" +
                "\n" +
                "Дальше необходимо реализовать функцию query_url, чтобы осуществлять поиск манги по уже готовой ссылке и получать по ним основные сведения такие как ссылка на основную страницу данной манги(для функции update), название, рейтинг, описание и т.д. Все эти данные запихиваете в ассоциативный массив и возвращаете список таких массивов.\n" +
                "List<Map<String,Object>> query(String url, int page)\n" +
                "\n" +
                "Дальше необходимо реализовать функцию update, чтобы обновлять данные по манге (точно так же как и в функции query так же получаете и запихиваете данные в объект Wrapper, только теперь ещё и основные данные по главам и вернуть нужно только один такой объект), а также получать данные о похожей манге (если есть возможность, не загружая другую страницу, а для таких случаев можно реализовать другую функцию которая указана ниже).\n" +
                "Map<String,Object> update(String url)\n" +
                "\n" +
                "Дальше необходимо реализовать функцию getPages, чтобы получать ссылки на страницы (изображения) главы, вернуть нужно список объектов Page.\n" +
                "List<Page> getPages(String url,Chapter chapter)\n" +
                "\n" +
                "Для расширенного поиска нужно реализовать функцию createAdvancedSearchOptions, которая будет возвращать список объектов Options.\n" +
                "List<Options> createAdvancedSearchOptions()\n" +
                "\n" +
                "Для загрузки данных похожей манги можете реализовать функцию loadSimilar, которая будет возвращать ArrayList<Wrapper> (если в функции update не возможно получить эти данные не загружая другую страницу).\n" +
                "List<Map<String,Object>> loadSimilar(Map<String,Object> manga)\n" +
                "\n"+"Весь проект вместе с примерами скриптов можно посмотреть на github: https://github.com/alex-bayir/Kitsune";
    }
}
