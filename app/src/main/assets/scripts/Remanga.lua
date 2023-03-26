---
--- Generated by Luanalysis
--- Created by Аlex Bayir.
--- DateTime: 09.01.2022 11:22
---

version="1.4"
domain="remanga.org"
source="Remanga"
Type="Manga"
description="Ещё один довольно популярный каталог манги."
host="https://api."..domain
auth_tokens={"user","token"}

Sorts={["По популярности"]="-rating", ["Новинки"]="-id", ["По обновлениям"]="-chapter_date",["По голосам"]="-votes",["По просмотрам"]="-views",["По количеству глав"]="-count_chapters"}
sorts={[1]="-rating",[2]="-id",[3]="-chapter_date"}
Genres={["Боевик"]="2", ["Боевые искусства"]="3", ["Гарем"]="5", ["Гендерная интрига"]="6", ["Героическое фэнтези"]="7", ["Детектив"]="8", ["Дзёсэй"]="9", ["Додзинси"]="10", ["Драма"]="11", ["Игра"]="12", ["История"]="13", ["Киберпанк"]="14", ["Кодомо"]="15", ["Комедия"]="50", ["Махо-сёдзё"]="17", ["Меха"]="18", ["Мистика"]="19", ["Научная фантастика"]="20", ["Повседневность"]="21", ["Постапокалиптика"]="22", ["Приключения"]="23", ["Психология"]="24", ["Романтика"]="25", ["Сверхъестественное"]="27", ["Сёдзё"]="28", ["Сёдзё-ай"]="29", ["Сёнэн"]="30", ["Сёнэн-ай"]="31", ["Спорт"]="32", ["Сэйнэн"]="33", ["Трагедия"]="34", ["Триллер"]="35", ["Ужасы"]="36", ["Фантастика"]="37", ["Фэнтези"]="38", ["Школа"]="39", ["Элементы юмора"]="16", ["Этти"]="40", ["Юри"]="41", ["Яой"]="43"}
Categories={["Веб"]="5",["В цвете"]="6",["Ёнкома"]="8",["Сборник"]="10",["Сингл"]="11",["Реинкарнация"]="13",["Зомби"]="14",["Демоны"]="15",["Кулинария"]="16",["Медицина"]="17",["Культивация"]="18",["Зверолюди"]="19",["Хикикомори"]="21",["Магия"]="22",["Горничные"]="23",["Мафия"]="24",["Средневековье"]="25",["Антигерой"]="26",["Призраки / Духи"]="27",["Гяру"]="28",["Военные"]="29",["Ниндзя"]="30",["Офисные работники"]="31",["Полиция"]="32",["Самураи"]="33",["Традиционные игры"]="34",["Видеоигры"]="35",["Преступники / Криминал"]="36",["Девушки-Монстры"]="37",["Монстры"]="38",["Музыка"]="39",["Обратный Гарем"]="40",["Выживание"]="41",["Путешествия во времени"]="43",["Виртуальная реальность"]="44",["Боги"]="45",["Эльфы"]="46",["Алхимия"]="47",["Ангелы"]="48",["Антиутопия"]="49",["Апокалипсис"]="50",["Армия"]="51",["Артефакты"]="52",["Борьба за власть"]="54",["Будущее"]="55",["Вестерн"]="56",["Владыка демонов"]="57",["Волшебные существа"]="59",["Воспоминания из другого мира"]="60",["Геймеры"]="61",["Гильдии"]="62",["ГГ женщина"]="63",["ГГ мужчина"]="64",["Гоблины"]="65",["Драконы"]="66",["Дружба"]="67",["Ранги силы"]="68",["Жестокий мир"]="69",["Животные компаньоны"]="70",["Завоевание мира"]="71",["Игровые элементы"]="73",["Квесты"]="75",["Космос"]="76",["Магическая академия"]="78",["Месть"]="79",["Навыки / способности"]="80",["Наёмники"]="81",["Насилие / жестокость"]="82",["Нежить"]="83",["Пародия"]="85",["Подземелья"]="86",["Политика"]="87",["Разумные расы"]="88",["Роботы"]="89",["Рыцари"]="90",["Система"]="91",["Стимпанк"]="92",["Скрытие личности"]="93",["Спасение мира"]="94",["Супергерои"]="95",["Учитель / ученик"]="96",["Философия"]="97",["Шантаж"]="99",["Лоли"]="108",["Тупой ГГ"]="109",["ГГ имба"]="110",["Умный ГГ"]="111",["Вампиры"]="112",["Оборотни"]="113",["Управление территорией"]="114",["Исекай"]="115",["Врачи / Доктора"]="116",["Аристократия"]="117",["Прокачка"]="118",["Амнезия / Потеря памяти"]="121",["Бои на мечах"]="122",["ГГ не человек"]="123",["Психодел-упоротость-Треш"]="124",["Грузовик-сан"]="125"}

function update(url)
    local jo=JSONObject:create(network:load(url)):getObject("content")
    if(jo~=nil) then
        local branches=jo:getArray("branches")
        local count=0; local branch
        for i=0,branches:size()-1,1 do
            local b=branches:size()>0 and branches:getObject(i) or nil
            local c=b~=nil and b:getInt("count_chapters") or 0
            if(c>count) then count=c; branch=b; end
        end
        local chapters={}; local n=0
        for page=math.ceil(count/100),1,-1 do
            local list=JSONObject:create(network:load(host.."/api/titles/chapters/?branch_id="..branch:getInt("id").."&count=100&page="..page)):getArray("content")
            for i=list:size()-1,0,-1 do
                local o=list:getObject(i); local p=o:getArray("publishers"):join(", ",{ "name"})
                chapters[n]=Chapter.new(o:get("tome"), o:get("chapter"),o:get("name"),utils:parseDate(o:get("upload_date"),"yyyy-MM-dd'T'HH:mm:ss"),utils:to_map({id=o:get("id"),translator=p}))
                n=n+1;
            end
        end
        return {
            ["url"]=url,
            ["url_web"]="https://"..domain.."/manga/"..jo:getString("dir"),
            ["name"]=jo:getString("en_name"),
            ["name_alt"]=jo:getString("rus_name"),
            ["genres"]=jo:getArray("genres"):join(", ",{"name"}),
            ["rating"]=jo:getDouble("avg_rating")/2,
            ["status"]=jo:getObject("status"):getString("name"),
            ["description"]=jo:getString("description"),
            ["thumbnail"]=host..jo:getObject("img"):getString("high"),
            ["chapters"]=chapters
        }
    end
end

function query(name,page,params)
    local url=network:url_builder(host.."/api/search"..(name and "/" or "/catalog/")):add("query",name):add("page",page+1)
    if(params and #params>0) then
        if(type(params[1])=="userdata" and Options:equals(params[1]:getClass())) then
            url:add("ordering",params[1]:getSelected()[1])
            if(#params>1) then url:add("genres",params[2]:getSelected()) end
            if(#params>2) then url:add("categories",params[3]:getSelected()) end
        else
            url:add("ordering",sorts[params[1]])
        end
    end
    url=url:build()
    return query_url(url)
end

function query_url(url,page)
    if page then url=url:find("page=") and url:gsub("page=%d+","page="..tostring(page+1)) or url.."&page="..tostring(page+1) end
    print(url)
    local array=JSONObject:create(network:load(url)):getArray("content")
    local list={}
    for i=0,array:size()-1,1 do
        local jo=array:get(i)
        list[i]={
            ["url"]=host.."/api/titles/"..jo:getString("dir"),
            ["url_web"]="https://"..domain.."/manga/"..jo:getString("dir"),
            ["name"]=jo:getString("en_name"),
            ["name_alt"]=jo:getString("rus_name"),
            ["rating"]=jo:getDouble("avg_rating")/2,
            ["thumbnail"]=host..jo:getObject("img"):getString("high")
        }
    end
    return list
end

function getPages(url,chapter) -- table <Page>
    local array=JSONObject:create(network:load((string.gsub(url,"[^/]+$","chapters/"..chapter["id"])))):getObject("content"):getArray("pages")
    local pages={}; local n=0
    for i=0,array:size()-1,1 do
        local jo=array:getObject(i)
        if(jo~=nil) then
            pages[n]=Page.new(jo:getInt("page"),jo:getString("link")); n=n+1
        else
            jo=array:getArray(i)
            local d=jo:size()<10 and 10 or (jo:size()<100 and 100 or 100);
            for j=0,jo:size()-1,1 do
                local tmp=jo:getObject(j)
                pages[n]=Page.new(tmp:getInt("page")+j/d,tmp:getString("link")); n=n+1
            end
        end
    end
    return pages
end

function createAdvancedSearchOptions() -- table <Options>
    return {
        Options.new("Сортировка",utils:to_map(Sorts),0),
        Options.new("Жанры",utils:to_map(Genres),1),
        Options.new("Категории",utils:to_map(Categories),1)
    }
end

function loadSimilar(manga)
    local array=JSONObject:create(network:load(manga["url"].."/similar")):getArray("content")
    local similar={}
    for i=0,array:size()-1,1 do
        local jo=array:getObject(i)
        similar[i]={
            ["url"]=host.."/api/titles/"..jo:getString("dir"),
            ["url_web"]="https://"..domain.."/manga/"..jo:getString("dir"),
            ["name"]=jo:getString("en_name"),
            ["name_alt"]=jo:getString("rus_name"),
            ["rating"]=jo:getDouble("avg_rating")/2,
            ["thumbnail"]=host..jo:getObject("img"):getString("high")
        }
    end
    return similar
end
