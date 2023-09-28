<div align="center">
    <h1>Kitsune scripts api</h1>
    (примеры скриптов смотрите в приложении или на github)</br>
    (принимаются предложения о том что добавить или как улучшить приложение)</br></br>
    Первым делом вам необходимо определить глобальные переменные:
</div>

| variable      | destination                                | example of value                                     |
|---------------|--------------------------------------------|------------------------------------------------------|
| `version`     | версия скрипта                             | 1.32                                                 |
| `domains`     | список доменов (основной и зеркала)        | \["mangalib.me", "mangalib.org"\]                    |
| `domain`      | провайдер                                  | mangalib.me                                          |
| `source`      | название источника                         | "MangaLib"                                           |
| `Type`        | тип                                        | "Manga" или "Ranobe"                                 |
| `description` | описание ресурса                           | "Этот ресурс ..."                                    |
| `auth_tokens` | токены для авторизации                     | \["mangalib_session", "XSRF-TOKEN"\]                 |
| `icon`        | ссылка на иконку источника (необязательно) | "https://mangalib.me/icons/android-icon-192x192.png" |

Дальше можно реализовать функцию `set_domain`, чтобы можно было менять домен на его зеркало, если основной не работает.
```lua
function set_domain(new_domain)
    domain=new_domain
    host="https://"..domain
    icon=host.."/icons/icon-192x192.png"
end 
```
Дальше необходимо реализовать функцию `query_url`, чтобы осуществлять поиск манги по уже готовой ссылке и получать по ним основные сведения такие как **ссылка на основную страницу данной книги** (манги, ранобе и других типов) (для функции `update`), название, рейтинг, описание и т.д. Все эти данные запихиваете в ассоциативный массив и возвращаете список таких массивов (для `Lua` ассоциативный массив это таблица).
```lua
function query_url(url,page)
    if not url:find(host.."/team") then
        if page and page>0 then url=url:find("page=") and url:gsub("page=%d+","page="..tostring(page+1)) or url.."&page="..tostring(page+1) end
    end
    local list={}
    ...
    return list
end 
```
Дальше необходимо реализовать функцию `query`, чтобы осуществлять поиск манги и получать по ним основные сведения такие как ссылка на основную страницу данной манги(для функции update), название, рейтинг, описание и т.д. Все эти данные запихиваете в ассоциативный массив и возвращаете список таких ассоциативных массивов. По возможности здесь должна формироваться полная ссылка, что бы потом вы могли передать её в `query_url` и от туда получить результат.
```lua
-- name - может быть как ссылкой так и названием тайтла
-- page - номер страницы поискового результата
-- params - нужен для расширенного поиска (передаются опции выбранные в AdvancedSearchOptions)
function query(name,page,params)
    url=host.."/search?q="..name.."sort="..params[2]:getSelected()[1]--......
    return query_url(url,page)
end 
```
Дальше необходимо реализовать функцию `update`, чтобы обновлять данные по манге (точно так же как и в функции `query` так же получаете и запихиваете данные в ассоциативный массив, только теперь ещё и основные данные по главам но вернуть уже нужно только один такой ассоциативный массив), а также получать данные о похожей манге (если есть возможность, не загружая другую страницу, а для таких случаев можно реализовать другую функцию которая указана ниже).
```lua
-- пример для Mangalib
function update(url)
    url=url:gsub("https?:[\\/][\\/][^\\/]+",host)
    local doc=network:load_as_Document(url)
    local json=JSONObject:create(doc:select("script"):toString():match("window.__DATA__ = (%b{})"))
    local ui=json:getObject("user"); if(ui) then ui=ui:get("id",-1); if(ui==-1) then ui=nil end end
    local jo=json:getObject("manga")
    local container=doc:selectFirst("div.media-container")
    local list=json:getObject("chapters"):getArray("list")
    local chapters={}
    local branches=json:getObject("chapters"):getArray("branches")
    local translators={}
    for i=0,(branches~=nil and branches:size() or 0)-1,1 do
        local teams=branches:getObject(i):getArray("teams")
        for j=0,(teams~=nil and teams:size() or 0)-1,1 do
            local o=teams:get(j)
            translators[o:getInt("branch_id")]={[utils:unescape_unicodes(o:get("name"))]=host.."/team/"..o:get("slug")}
        end
    end
    for i=list:size()-1,0,-1 do
        local o=list:getObject(i); local branch_id=o:get("branch_id",-1); if(branch_id==-1) then branch_id=nil end
        chapters[#chapters+1]={vol=o:get("chapter_volume"),num=o:get("chapter_number"),name=utils:unescape_unicodes(o:get("chapter_name")),date=utils:parseDate(o:get("chapter_created_at"),"yyyy-MM-dd' 'HH:mm:ss"),translators=translators[branch_id] or "",bid=branch_id,ui=ui}
    end
    local author=container:selectFirst("a[abs:href*=/people/]")
    return {
        ["url"]=url,
        ["url_web"]=url,
        ["name"]=jo:getString("name"),
        ["name_alt"]=jo:getString("rus_name"),
        ["author"]=author and {[utils:text(author)]=utils:attr(author,"abs:href")},
        ["genres"]=utils:text(container:select("a.media-tag-item"):select("a[href*=?genres]"),"",", "),
        ["tags"]=utils:text(container:select("a.media-tag-item"):select("a[href*=?tags]"),"",", "),
        ["status"]=status(container:select("a[href*=manga_status]"):attr("href"):match("manga_status.*=(%d)")),
        ["rating"]=num(container:selectFirst("div.media-rating__value"):text())/2,
        ["description"]=utils:attr(container:selectFirst("div.media-section_info"):getElementsByAttributeValue("itemprop","description"):first(),"content"),
        ["thumbnail"]=container:selectFirst("div.media-sidebar__cover.paper"):selectFirst("img"):attr("src"),
        ["chapters"]=unique(chapters,nil),
        ["similar"]=similar(container:select("div.media-slider__item"))
    }
end
```

Дальше необходимо реализовать функцию `getPages`, чтобы получать ссылки на страницы (изображения) (однако вместо ссылок можно передавать текст если в главе содержатся не только картинки) главы, вернуть нужно массив объектов. Где каждая страница это ассоциативный массив с `page` и `data`, где `page` - номер страницы, а `data` - это либо ссылка либо текст.
```lua
-- пример для Mangalib
function getPages(url,chapter)
    local scripts=network:load_as_Document(network:url_builder(url.."/v"..chapter["vol"].."/c"..chapter["num"]):add("page",1):add("bid",chapter["bid"]):add("ui",chapter["ui"]):build()):select("script")
    local json=JSONObject:create(scripts:toString():match("window.__info = (.-);"))
    local array=JSONArray:create(scripts:toString():match("window.__pg = (.-);"))
    local domain=json:getObject("servers"):getString(json:getObject("img"):getString("server")).."/"..json:getObject("img"):getString("url")
    local pages={}
    for i=0,array:size()-1,1 do
        local jo=array:getObject(i)
        pages[i]={["page"]=jo:getInt("p"),["data"]=domain..jo:getString("u")}
    end
    return pages
end
```
Дальше необходимо реализовать функцию `load`, чтобы скачивать страницы по полученным ссылкам. Этот метод предназначен для исправления автоматического исправления ссылок если произошла ошибка скачивания, если это необходимо.
```lua
-- пример для Mangalib

-- data - это ссылка или текст
-- url - true если data это ссылка и false если нет
-- cancel_flag и progress внутренние переменные приложения которые необходимо передать без изменений
function load(file,data,url,cancel_flag,progress)
    local error=network:load(network:getClient(),data,domain,file,cancel,process)
    if(error and error:getMessage()=="Length of data is zero") then
        for key,value in pairs(alt_urls(data)) do
            if(network:load(network:getClient(),value,domain,file,cancel,process)==nil) then
                return nil
            end
        end
    end
    return error
end
```
Для расширенного поиска нужно реализовать функцию `createAdvancedSearchOptions`, которая будет возвращать список специально подготовленных ассоциативных массивов.
```lua
-- пример для Mangalib
Sorts={["По рейтингу"]="rating_score", ["По количеству оценок"]="rate", ["По названию"]="name", ["По дате обновлениям"]="last_chapter_at",["По дате добавления"]="created_at",["По просмотрам"]="views",["По количеству глав"]="chap_count"}
...
-- mode=0 - Список с выбором одного элемента (RadioGroup) и CheckBox для выбора сортировки
-- mode=1 - Список с выбором одного элемента (RadioGroup)
-- mode=2 - Список с выбором элементов (обычный CheckBox)
-- mode=3 - Список с выбором элементов (CheckBox с тремя сострояниями: не выбрано, выбрано, исключено)
-- mode=4 - Два значения start и end (только числа)
-- mode=5 - Поле для ввода строки
-- title  - заголовок списка
-- values - опции для фильтрации поиска
-- desc и asc - ключи опций для сортировки по убыванию и возрастанию (нужно указать оба)
function createAdvancedSearchOptions()
    return {
        {mode=0,title="Сортировка",desc="desc",asc="asc",values=Sorts},
        {mode=4,title="Количество глав"},
        {mode=4,title="Год выпуска"},
        {mode=4,title="Оценка"},
        {mode=2,title="Жанры",values=Genres},
        {mode=2,title="Теги",values=Tags},
        {mode=1,title="Серии",values=Series},
        {mode=1,title="Типы",values=Types},
        {mode=1,title="Статус тайтла",values=Status},
        {mode=2,title="Формат",values=Formats}
    }
end
```
Для загрузки данных похожей манги можете реализовать функцию `loadSimilar`, которая будет возвращать то же что и `query_url` (реализация нужна, если в функции `update` не возможно получить список похожих не загружая другую страницу).
```lua
-- пример для Desu
-- book - это json хранящий всю информацию о тайтле
function loadSimilar(book)
    local elements=network:load_as_Document(host.."/manga/"..book["id"]):select("article.c-anime")
    local similar={}; local n=0;
    for i=0,(elements and elements:size() or 0)-1,1 do
        local e=elements:get(i):selectFirst("img")
        similar[n]=e and {
            ["url"]=host.."/manga/api/"..e:attr("src"):match("%d+"),
            ["id"]=num(e:attr("src"):match("%d+")),
            ["name"]=e:attr("title"),
            ["name_alt"]=e:attr("title"),
            ["thumbnail"]=host..e:attr("src")
        }
        n=n+(e and 1 or 0)
    end
    return similar
end
```

> [!NOTE]
> Модули которые вы можете использовать в Lua ():

```lua
network -- (для создания запросов)

---@return Throwable -- ошибка призапросе
function network:load(client,url,domain,file,cancel_flag,listener) end

---@return Throwable -- ошибка призапросе
function network:load(client,url,headers,file,cancel_flag,listener) end

---@return Throwable -- ошибка призапросе
function network:load(client,url,domain,headers,file,cancel_flag,listener) end

---@return string
function network:load(url,headers) end

---@return string
function network:load_as_String(url,headers,body,type) end

---@return org.jsoup.nodes.Document
function network:load_as_Document(url,headers,body) end

---@return org.alex.kitsune.commons.URLBuilder
function network:url_builder(host) end

---@return org.jsoup.nodes.Document
function network:parse(html) end

---@return OkHtttpClient
function network:getClient() end

---watch source MangaTeaderTo
---@param descrabmle boolean
---@return OkHtttpClient
function network:getClient(descramble) end

---@return string
function network:getCookie(domain,key) end

---@param cookies List<Cookie>
---@return string
function network:getCookie(cookies,key) end

---decode url special chars
---@return string
function network:decode(encoded) end

---encode url special chars
---@return string
function network:encode(decoded) end

utils -- различные вспомогательные функции

---запись в файл
---@param file java.io.File
---@param text string
---@param append boolean
---@return Throwable
function utils:write(file,text,append) end

---@return string
function utils:unescape_unicodes(escaped) end

---date to timestamp
---@return long
function utils:parseDate(date,format) end

---для исключение nullpointerexception
---@param element org.jsoup.nodes.Element 
---@param attr string
---@param def string не обязателен
---@return string
function utils:attr(element,attr,def) end
---для исключение nullpointerexception
---@param element org.jsoup.nodes.Element
---@param def string не обязателен
---@return string
function utils:text(element,def) end

---для исключение nullpointerexception
---@param elements org.jsoup.nodes.Elements
---@param attr string
---@param def string не обязателен
---@param delimeter string не обязателен
---@return string
function utils:attr(elements,attr,def,delimeter) end

---для исключение nullpointerexception
---@param elements org.jsoup.nodes.Elements
---@param def string не обязателен
---@param delimeter string не обязателен
---@return string
function utils:text(elements,def,delimeter) end

JSONObject (обычный json объект только унаследованный от TreeMap)
JSONArray (обычный json массив только унаследованный от LinkedList)
```