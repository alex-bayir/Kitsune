---
--- Generated by Luanalysis
--- Created by Аlex Bayir.
--- DateTime: 09.01.2022 11:22
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
provider="desu.me"
providerName="Desu"
sourceDescription="Один из лучших каталогов манги. Хорош тем, что на сайте быстро заливают новые главы."
host="https://"..provider
auth_tokens={"xf_user","xf_session"}

Sorts={["По популярности"]="popular", ["По добавлению"]="id",["По алфавиту"]="name", ["По обновлениям"]="updated"}
sorts={[1]="popular",[2]="id",[3]="updated"}
Genres={["Безумие"]="dementia",["Боевые искусства"]="martial arts",["В цвете"]="color",["Вампиры"]="vampire",["Веб"]="web",["Гарем"]="harem",["Героическое фэнтези"]="heroic fantasy",["Демоны"]="demons",["Детектив"]="mystery",["Дзёсей"]="josei",["Драма"]="drama",["Ёнкома"]="yonkoma",["Игры"]="game",["Исекай"]="isekai",["Исторический"]="historical",["Комедия"]="comedy",["Космос"]="space",["ЛитRPG"]="litrpg",["Магия"]="magic",["Меха"]="mecha",["Мистика"]="mystic",["Музыка"]="music",["Научная фантастика"]="sci-fi",["Пародия"]="parody",["Повседневность"]="slice of life",["Постапокалиптика"]="post apocalyptic",["Приключения"]="adventure",["Психологическое"]="psychological",["Романтика"]="romance",["Самураи"]="samurai",["Сверхъестественное"]="supernatural",["Сёдзе"]="shoujo",["Сёдзе Ай"]="shoujo ai",["Сейнен"]="seinen",["Сёнен"]="shounen",["Сёнен Ай"]="shounen ai",["Смена пола"]="gender bender",["Спорт"]="sports",["Супер сила"]="super power",["Трагедия"]="tragedy",["Триллер"]="thriller",["Ужасы"]="horror",["Фантастика"]="fiction",["Фэнтези"]="fantasy",["Хентай"]="hentai",["Школа"]="school",["Экшен"]="action",["Этти"]="ecchi",["Юри"]="yuri",["Яой"]="yaoi"}

function update(url) -- Wrapper
    local jo=JSONObject.new(Wrapper:loadPage(url)):getJSONObject("response")
    local list=jo:getJSONObject("chapters"):getJSONArray("list")
    local chapters=ArrayList.new(list:length())
    for i=list:length()-1,0,-1 do
        chapters:add(Chapter.new(list:getJSONObject(i),"id","vol","ch","title","date",1000))
    end
    local genres=jo:getJSONArray("genres") local str="" for i=0,genres:length()-1,1 do str=str..", "..genres:getJSONObject(i):getString("russian") end genres=str:sub(3)
    return Wrapper.new(
            url,
            jo:getInt("id"),
            jo:getString("name"),
            jo:getString("russian"),
            nil,
            nil,
            genres,
            jo:getDouble("score")/2,
            jo:getString("status"),
            jo:getString("description"),
            jo:getJSONObject("image"):getString("x225"),
            jo:getString("url"),
            chapters
    )
end
function query(name,page,params) -- java.util.ArrayList<Wrapper>
    local url=UrlBuilder.new(host.."/manga/api/")
    url:add("limit",100)
    url:add("search",name)
    url:add("page",page+1)
    if(params~=nil and #params>0) then
        if(type(params[1])=="userdata" and Options:equals(params[1]:getClass())) then
            url:add("order",params[1]:getSelected()[1])
            if(#params>1) then url:add("genres",params[2]:getSelected(),',') end
        else
            url:add("order",sorts[params[1]])
        end
    end
    url=url:build()
    print(url)
    local array=JSONObject.new(Wrapper:loadPage(url)):getJSONArray("response")
    local list=ArrayList.new(array:length())
    for i=0,array:length()-1,1 do
        local jo=array:get(i)
        list:add(Wrapper.new(
                host.."/manga/api/"..jo:getInt("id"),
                jo:getInt("id"),
                jo:getString("name"),
                jo:getString("russian"),
                nil,
                nil,
                jo:getString("genres"),
                jo:getDouble("score")/2,
                jo:getString("status"),
                jo:getString("description"),
                jo:getJSONObject("image"):getString("x225")
        ))
    end
    return list
end
function getPages(url,chapter) -- ArrayList<Page>
    local array=JSONObject.new(Wrapper:loadPage(url.."/chapter/"..chapter.id)):getJSONObject("response"):getJSONObject("pages"):getJSONArray("list")
    local pages=ArrayList.new(array:length())
    for i=0,array:length()-1,1 do
        local jo=array:getJSONObject(i)
        pages:add(Page.new(jo:getInt("id"),jo:getInt("page"),jo:getString("img")))
    end
    return pages
end
function createAdvancedSearchOptions() -- ArrayList<Options>
    local options=ArrayList.new()
    options:add(Options.new("Сортировка",convert(Sorts),0))
    options:add(Options.new("Жанры",convert(Genres),1))
    return options
end

function convert(luaTable)
    local javaTable=Map_class.new()
    for key,value in pairs(luaTable) do javaTable:put(key,value) end
    return javaTable
end

function num(n) return n==nil and 0 or tonumber(n:match("[0-9]*%.?[0-9]+")) end

function loadSimilar(wrapper)
    local elements=Wrapper:loadDocument(host.."/manga/"..wrapper.id):select("article.c-anime")
    local similar=ArrayList.new()
    for i=0,(elements~=nil and elements:size() or 0)-1,1 do
        local e=elements:get(i):selectFirst("img")
        if(e~=nil) then
            similar:add(Wrapper.new(
                    host.."/manga/api/"..e:attr("src"):match("%d+"),
                    num(e:attr("src"):match("%d+")),
                    e:attr("title"),
                    e:attr("title"),
                    nil,
                    nil,
                    nil,
                    0,
                    nil,
                    nil,
                    host..e:attr("src")
            ))
        end
    end
    return similar
end