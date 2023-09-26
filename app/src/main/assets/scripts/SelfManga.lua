---
--- Generated by Luanalysis
--- Created by Аlex Bayir.
--- DateTime: 09.01.2022 19:08
---

version="1.5"
domains={"selfmanga.live"}
domain=domains[1]
source="SelfManga"
Type="Manga"
description="На этом источнике размещается только русская авторская манга и журналы о манге."
host="https://"..domain
auth_tokens={"JSESSIONID"}

Filters={["Высокий рейтинг"]="s_high_rate", ["Переведено"]="s_translated", ["Завершённая"]="s_completed",["Для взрослых"]="s_mature",["Сингл"]="s_single",["Длинная"]="s_many_chapters",["Ожидает загрузки"]="s_wait_upload",["Продаётся"]="s_sale"}
filters={[1]="s_high_rate"}
Genres={["Боевик"]="el_2155",["Боевые искусства"]="el_2143",["Гарем"]="el_2142",["Гендерная интрига"]="el_2156",["Героическое фэнтези"]="el_2146",["Детектив"]="el_2152",["Дзёсэй"]="el_2158",["Драма"]="el_2118",["Игра"]="el_2154",["История"]="el_2119",["Исэкай"]="el_9450",["Киберпанк"]="el_8032",["Кодомо"]="el_2137",["Комедия"]="el_2136",["Махо-сёдзё"]="el_2147",["Меха"]="el_2126",["Научная фантастика"]="el_2133",["Повседневность"]="el_2135",["Постапокалиптика"]="el_2151",["Приключения"]="el_2130",["Психология"]="el_2144",["Романтика"]="el_2121",["Самурайский боевик"]="el_2124",["Сверхъестественное"]="el_2159",["Сёдзё"]="el_2122",["Сёдзё-ай"]="el_2128",["Сёнэн"]="el_2134",["Сёнэн-ай"]="el_2139",["Спорт"]="el_2129",["Сэйнэн"]="el_2138",["Сянься"]="el_9561",["Трагедия"]="el_2153",["Триллер"]="el_2150",["Ужасы"]="el_2125",["Уся"]="el_9560",["Фэнтези"]="el_2131",["Школа"]="el_2127",["Этти"]="el_2149",["Юри"]="el_2123",["Яой"]="el_6001"}
Tags={["Средневековье"]="5988",["Спортивное тело"]="5987",["Легендарное оружие"]="5951",["Зверолюди"]="5941",["Вестерн"]="5916",["Подземелья"]="5972",["Виртуальная реальность"]="5918",["Борьба за власть"]="5912",["Волшебные существа"]="5923",["Космос"]="5947",["Жестокий мир"]="5938",["Рыцари"]="5982",["Брат и сестра"]="5913",["Гоблины"]="5932",["Зомби"]="5943",["Боги"]="5910",["Волшебники"]="5922",["Офисные Работники"]="5969",["Нежить"]="5964",["Завоевание мира"]="5940",["Наёмники"]="5963",["Обмен телами"]="5966",["ГГ мужчина"]="5928",["Ангелы"]="5904",["Самураи"]="5983",["Антиутопия"]="5906",["Полиция"]="5974",["Насилие"]="5962",["Монстры"]="5959"}

function set_domain(new_domain)
    domain=new_domain
    host="https://"..domain
end

function update(url)
    url=url:gsub("https?:[\\/][\\/][^\\/]+",host)
    local e=network:load_as_Document(url):selectFirst("div.leftContent")
    local list=e:select("table.table-hover"):select("tr.item-row")
    local chapters={}; local last=list:size()-1
    for i=last,0,-1 do
        local elem=list:get(i);
        chapters[last-i]={vol=num(elem:attr("data-vol")),num=num(elem:attr("data-num"))/10,name=elem:select("td.item-title"):text():match("%d+%s%-%s%d+%s+(.+)"),date=utils:parseDate(elem:select("td.date"):attr("data-date"),"dd.MM.yy")}
    end
    local author={}; local authors=e:select("span.elem_author"):select("a[href~=/list/person/]")
    for j=0,authors:size()-1,1 do
        local a=authors:get(j); author[a:text()]=a:attr("abs:href")
    end
    return {
        ["url"]=url,
        ["url_web"]=url,
        ["name"]=utils:text(e:selectFirst("span.eng-name"),e:selectFirst("span.name"):text()),
        ["name_alt"]=utils:text(e:selectFirst("span.name")),
        ["author"]=author,
        ["genres"]=utils:text(e:select("div.subject-meta"):select("a[href*=/genre/]"),"",", "),
        ["tags"]=utils:text(e:select("div.subject-meta"):select("a[href*=/tag/]"),"",", "),
        ["rating"]=num(e:selectFirst("span.rating-block"):attr("data-score")),
        ["status"]=status(utils:text(e:selectFirst("div.subject-meta"):selectFirst("span.text-info"))),
        ["description"]=utils:text(e:selectFirst("div.manga-description")),
        ["thumbnail"]=e:selectFirst("div.picture-fotorama"):selectFirst("img"):attr("src"),
        ["chapters"]=chapters,
        ["similar"]=similar(e:select("div[class~=(tile|simple-tile)]"))
    }
end

function query(name,page,params)
    local url=network:url_builder(host.."/search/advancedResults"):add("q",name):add("offset",page*50)
    if(params~=nil and #params>0) then
        if(type(params[1])=="userdata" and Options:equals(params[1]:getClass())) then
            url:add(params[1]:getSelected(),"in"):add(params[1]:getDeselected(),"ex")
            if(#params>1) then url:add("many-el_40",params[2]:getSelected()) end
            if(#params>2) then url:add(params[3]:getSelected(),"in"):add(params[3]:getDeselected(),"ex") end
        else
            url:add(filters[params[1]],"in")
        end
    end
    url=url:build()
    if(name~=nil and name:match("[a-z]://[^ >,;]*")~=nil) then url=name; end
    return query_url(url)
end

function query_url(url,page)
    if page then url=url:find("offset=") and url:gsub("offset=%d+","offset="..tostring(page*50)) or url.."&offset="..tostring(page*50) end
    print(url)
    local selects=network:load_as_Document(url):select("div.tile")
    local list={}
    for i=0,selects:size()-1,1 do
        local e=selects:get(i)
        local author={}; local authors=e:select("span.elem_author"):select("a[href~=/list/person/]")
        for j=0,authors:size()-1,1 do
            local a=authors:get(j); author[a:text()]=a:attr("abs:href")
        end
        list[i]={
            ["url"]=e:selectFirst("a.non-hover"):attr("abs:href"),
            ["url_web"]=e:selectFirst("a.non-hover"):attr("abs:href"),
            ["name"]=utils:text(e:selectFirst("h4"),utils:text(e:selectFirst("h3"))),
            ["name_alt"]=utils:text(e:selectFirst("h3")),
            ["author"]=author,
            ["genres"]=utils:text(e:select("a.element-link"),""):gsub(" ",", "),
            ["rating"]=num(utils:attr(e:selectFirst("div.rating"),"title",""):match("(.-)%s"))/2,
            ["description"]=utils:text(e:selectFirst("div.manga-description")),
            ["thumbnail"]=e:selectFirst("img.lazy"):attr("data-original")
        }
    end
    return list
end

function getPages(url,chapter)
    local array=JSONArray:create(network:load_as_Document(url.."/vol"..chapter["vol"].."/"..chapter["num"].."?mtr=1"):select("script"):toString():match("rm_h.initReader.*(%[%[.*%]%])"):gsub("\'","\""))
    local pages={}
    for i=0,array:size()-1,1 do
        local ja=array:getArray(i)
        pages[i]={["page"]=i+1,["data"]=(ja:getString(0)..ja:getString(2))}
    end
    return pages
end

function load(file,data,url,cancel,process)
    local error=network:load(network:getClient(),data,domain,file,cancel,process)
    if(error~=nil and network:load(network:getClient(),data:match("([^?]+)"),domain,file,cancel,process)==nil) then
        return nil
    end
    return error
end

function createAdvancedSearchOptions()
    return {
        {mode=2,title="Жанры",values=Genres},
        {mode=1,title="Теги",values=Tags},
        {mode=2,title="Фильтры",values=Filters}
    }
end

function similar(elements)
    local similar={}; local n=0
    for i=0,(elements~=nil and elements:size() or 0)-1,1 do
        local e=elements:get(i):selectFirst("a")
        local img=e and e:selectFirst("img.lazy")
        similar[n]=img and {
            ["url"]=e:attr("abs:href"),
            ["url_web"]=e:attr("abs:href"),
            ["name"]=img:attr("alt"),
            ["name_alt"]=img:attr("title"),
            ["thumbnail"]=img:attr("data-original")
        }
        n=n+(img and 1 or 0)
    end
    return similar
end

function status(status)
    return ({["запланирован"]="Announce",["выпуск продолжается"]="Ongoing",["приостановлен"]="Paused",["прекращён"]="Stopped",["завершено"]="Finished"})[status]
end