---
--- Generated by Luanalysis
--- Created by Аlex Bayir.
--- DateTime: 09.01.2022 19:08
---
Wrapper=luajava.bindClass("org.alex.kitsune.manga.Wrapper")
Chapter=luajava.bindClass("org.alex.kitsune.manga.Chapter")
Page=luajava.bindClass("org.alex.kitsune.manga.Page")
Options=luajava.bindClass("org.alex.kitsune.manga.search.Options")
UrlBuilder=luajava.bindClass("org.alex.kitsune.commons.URLBuilder")
JSONObject=luajava.bindClass("org.json.JSONObject")
JSONArray=luajava.bindClass("org.json.JSONArray")
ArrayList=luajava.bindClass("java.util.ArrayList")
Map_class=luajava.bindClass("java.util.TreeMap")

version="1.3"
provider="mintmanga.live"
providerName="MintManga"
sourceDescription="Источник манги для взрослых. Присутствует яой. Пожалуйста отключите этот источник если вам меньше 18. Также рекомендуется его отключить,если в рекомендациях попадается яой манга."
host="https://"..provider
auth_tokens={"JSESSIONID"}

Filters={["Высокий рейтинг"]="s_high_rate", ["Переведено"]="s_translated", ["Завершённая"]="s_completed",["Для взрослых"]="s_mature",["Сингл"]="s_single",["Длинная"]="s_many_chapters",["Ожидает загрузки"]="s_wait_upload",["Продаётся"]="s_sale"}
filters={[1]="s_high_rate"}
Genres={["Боевик"]="el_1346",["Боевые искусства"]="el_1334",["Гарем"]="el_1333",["Гендерная интрига"]="el_1347",["Героическое фэнтези"]="el_1337",["Детектив"]="el_1343",["Дзёсэй"]="el_1349",["Драма"]="el_1310",["Игра"]="el_5229",["История"]="el_1311",["Исэкай"]="el_6420",["Киберпанк"]="el_1351",["Комедия"]="el_1328",["Меха"]="el_1318",["Научная фантастика"]="el_1325",["Омегаверс"]="el_5676",["Повседневность"]="el_1327",["Постапокалиптика"]="el_1342",["Приключения"]="el_1322",["Психология"]="el_1335",["Романтика"]="el_1313",["Самурайский боевик"]="el_1316",["Сверхъестественное"]="el_1350",["Сёдзё"]="el_1314",["Сёдзё-ай"]="el_1320",["Сёнэн"]="el_1326",["Сёнэн-ай"]="el_1330",["Спорт"]="el_1321",["Сэйнэн"]="el_1329",["Сянься"]="el_6631",["Трагедия"]="el_1344",["Триллер"]="el_1341",["Ужасы"]="el_1317",["Уся"]="el_6632",["Фэнтези"]="el_1323",["Школа"]="el_1319",["Эротика"]="el_1340",["Этти"]="el_1354",["Юри"]="el_1315",["Яой"]="el_1336"}
Tags={["Спортивное тело"]="6612",["Спасение мира"]="6611",["Офисные Работники"]="6594",["Традиционные игры"]="6616",["Ранги силы"]="6604",["ГГ женщина"]="6551",["Остров"]="6639",["Разумные расы"]="6603",["Культивация"]="6574",["Ангелы"]="6529",["Демоны"]="6560",["Злые духи"]="6567",["ГГ мужчина"]="6553",["ГГ имба"]="6552",["Волшебники"]="6547",["Игровые элементы"]="6569",["Система"]="6609",["Якудза"]="6624",["Брат и сестра"]="6538",["Империи"]="6570",["Месть"]="6582",["Медицина"]="6581",["Мафия"]="6580",["Насилие"]="6587",["Путешествие во времени"]="6602",["Амнезия"]="6528",["Средневековье"]="6613",["Гильдии"]="6555",["Магия"]="6579"}

function update(url) -- Wrapper
    local e=Wrapper:loadDocument(url):selectFirst("div.leftContent")
    local list=e:select("table.table-hover"):select("tr")
    local chapters=ArrayList.new(list:size())
    for i=list:size()-1,0,-1 do
        local tmp=list:get(i)
        local href=tmp:selectFirst("a"):attr("href")
        chapters:add(Chapter.new(num(href:match("%d+")),num(href:match("/vol(%d+)")),num(href:match("/vol%d+/(%d*%.?%d*)")), tmp:select("td"):first():text():match("%d+%s%-%s%d+%s+(.+)"),Wrapper:parseDate(tmp:select("td"):last():attr("data-date"),"dd.MM.yy")))
    end
    return Wrapper.new(
            url,
            0,
            Wrapper:text(e:selectFirst("span.eng-name"),e:selectFirst("span.name"):text()),
            Wrapper:text(e:selectFirst("span.name")),
            Wrapper:text(e:select("span.elem_author")),
            Wrapper:attr(e:select("span.elem_author"):select("a"),"abs:href"),
            Wrapper:text(e:select("span.elem_genre"),""):gsub(" ,",","),
            num(e:selectFirst("span.rating-block"):attr("data-score")),
            Wrapper:text(e:select("div.subject-meta"):select("p"):get(1)),
            Wrapper:text(e:selectFirst("div.manga-description")),
            e:selectFirst("div.picture-fotorama"):selectFirst("img"):attr("src"),
            url,
            chapters,
            similar(e:select("div[class~=(tile|simple-tile)]"))
    )
end

function query(name,page,params) -- java.util.ArrayList<Wrapper>
    local url=UrlBuilder.new(host.."/search/advanced")
    url:add("q",name)
    url:add("page",page+1)
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
    print(url)
    local selects=Wrapper:loadDocument(url):select("div.tile")
    local list=ArrayList.new(selects:size())
    for i=0,selects:size()-1,1 do
        local e=selects:get(i)
        if(e:selectFirst("a.non-hover")~=nil) then
            list:add(Wrapper.new(
                    e:selectFirst("a.non-hover"):attr("abs:href"),
                    0,
                    Wrapper:text(e:selectFirst("h4"),Wrapper:text(e:selectFirst("h3"))),
                    Wrapper:text(e:selectFirst("h3")),
                    Wrapper:text(e:select("a.person-link"),""):gsub(" ,",","),
                    Wrapper:attr(e:selectFirst("a.person-link"),"abs:href"),
                    Wrapper:text(e:select("a.element-link"),""):gsub(" ",", "),
                    num(Wrapper:attr(e:selectFirst("div.rating"),"title",""):match("(.-)%s"))/2,
                    0,
                    Wrapper:text(e:selectFirst("div.manga-description")),
                    e:selectFirst("img.lazy"):attr("data-original")
            ))
        end
    end
    return list
end

function getPages(url,chapter) -- ArrayList<Page>
    local array=JSONArray.new(Wrapper:loadDocument(url.."/vol"..chapter.vol.."/"..chapter.num.."?mtr=1"):select("script"):toString():match("rm_h.initReader.*(%[%[.*%]%])"):gsub("\'","\""))
    local pages=ArrayList.new(array:length())
    for i=0,array:length()-1,1 do
        local ja=array:getJSONArray(i)
        pages:add(Page.new(0,i+1,ja:getString(0)..ja:getString(2)))
    end
    return pages
end

function createAdvancedSearchOptions() -- ArrayList<Options>
    local options=ArrayList.new()
    options:add(Options.new("Жанры",convert(Genres),2))
    options:add(Options.new("Теги",convert(Tags),1))
    options:add(Options.new("Фильтры",convert(Filters),2))
    return options
end

function convert(luaTable)
    local javaTable=Map_class.new()
    for key,value in pairs(luaTable) do javaTable:put(key,value) end
    return javaTable
end

function num(n) return n==nil and 0 or tonumber(n:match("[0-9]*%.?[0-9]+")) end

function similar(elements)
    local similar=ArrayList.new()
    for i=0,(elements~=nil and elements:size() or 0)-1,1 do
        local e=elements:get(i):selectFirst("a")
        if(e~=nil) then
            local img=e:selectFirst("img.lazy")
            if(img~=nil) then
                similar:add(Wrapper.new(
                        e:attr("abs:href"),
                        0,
                        img:attr("alt"),
                        img:attr("title"),
                        nil,
                        nil,
                        nil,
                        0,
                        nil,
                        nil,
                        img:attr("data-original")
                ))
            end
        end
    end
    return similar
end