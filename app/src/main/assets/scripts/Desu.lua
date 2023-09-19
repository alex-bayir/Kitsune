---
--- Generated by Luanalysis
--- Created by Аlex Bayir.
--- DateTime: 09.01.2022 11:22
---

version="1.5"
domains={"desu.me","desu.win"}
domain=domains[1]
source="Desu"
Type="Manga"
description="Один из лучших каталогов манги. Хорош тем, что на сайте быстро заливают новые главы."
host="https://"..domain
auth_tokens={"xf_user","xf_session"}
icon=host.."/favicon.ico"

Sorts={["По популярности"]="popular", ["По добавлению"]="id",["По алфавиту"]="name", ["По обновлениям"]="updated"}
sorts={[1]="popular",[2]="id",[3]="updated"}
Genres={["Безумие"]="dementia",["Боевые искусства"]="martial arts",["В цвете"]="color",["Вампиры"]="vampire",["Веб"]="web",["Гарем"]="harem",["Героическое фэнтези"]="heroic fantasy",["Демоны"]="demons",["Детектив"]="mystery",["Дзёсей"]="josei",["Драма"]="drama",["Ёнкома"]="yonkoma",["Игры"]="game",["Исекай"]="isekai",["Исторический"]="historical",["Комедия"]="comedy",["Космос"]="space",["ЛитRPG"]="litrpg",["Магия"]="magic",["Меха"]="mecha",["Мистика"]="mystic",["Музыка"]="music",["Научная фантастика"]="sci-fi",["Пародия"]="parody",["Повседневность"]="slice of life",["Постапокалиптика"]="post apocalyptic",["Приключения"]="adventure",["Психологическое"]="psychological",["Романтика"]="romance",["Самураи"]="samurai",["Сверхъестественное"]="supernatural",["Сёдзе"]="shoujo",["Сёдзе Ай"]="shoujo ai",["Сейнен"]="seinen",["Сёнен"]="shounen",["Сёнен Ай"]="shounen ai",["Смена пола"]="gender bender",["Спорт"]="sports",["Супер сила"]="super power",["Трагедия"]="tragedy",["Триллер"]="thriller",["Ужасы"]="horror",["Фантастика"]="fiction",["Фэнтези"]="fantasy",["Хентай"]="hentai",["Школа"]="school",["Экшен"]="action",["Этти"]="ecchi",["Юри"]="yuri",["Яой"]="yaoi"}

function set_domain(new_domain)
    domain=new_domain
    host="https://"..domain
    icon=host.."/favicon.ico"
end

function update(url)
    url=url:gsub("https?:[\\/][\\/][^\\/]+",host)
    local jo=JSONObject:create(network:load(url)):getObject("response")
    local list=jo:getObject("chapters"):getArray("list")
    local chapters={}; local last=list:size()-1;
    for i=last,0,-1 do
        local o=list:getObject(i)
        chapters[last-i]=Chapter.new(o:get("vol"), o:get("ch"), o:get("title"), o:get("date")*1000,utils:to_map({id=o:get("id")}))
    end
    return {
        ["url"]=url,
        ["id"]=jo:getInt("id"),
        ["url_web"]=jo:getString("url"),
        ["name"]=jo:getString("name"),
        ["name_alt"]=jo:getString("russian"),
        ["genres"]=jo:getArray("genres"):join(", ",{"russian"}),
        ["rating"]=jo:getDouble("score")/2,
        ["status"]=status(jo:getString("status")),
        ["description"]=jo:getString("description"),
        ["thumbnail"]=jo:getObject("image"):getString("x225"),
        ["chapters"]=chapters
    }
end

function query(name,page,params)
    local url=network:url_builder(host.."/manga/api/")
    :add("limit",100)
    :add("search",name)
    :add("page",page+1)
    if(params~=nil and #params>0) then
        if(type(params[1])=="userdata" and Options:equals(params[1]:getClass())) then
            url:add("order",params[1]:getSelected()[1])
            if(#params>1) then url:add("genres",params[2]:getSelected(),',') end
        else
            url:add("order",sorts[params[1]])
        end
    end
    url=url:build()
    return query_url(url)
end

function query_url(url,page)
    if page then url=url:find("page=") and url:gsub("page=%d+","page="..tostring(page+1)) or url.."&page="..tostring(page+1) end
    print(url)
    local array=JSONObject:create(network:load(url)):getArray("response")
    local list={}
    for i=0,array:size()-1,1 do
        local jo=array:get(i)
        list[i]={
            ["url"]=host.."/manga/api/"..jo:getInt("id"),
            ["id"]=jo:getInt("id"),
            ["name"]=jo:getString("name"),
            ["name_alt"]=jo:getString("russian"),
            ["genres"]=jo:getString("genres"),
            ["rating"]=jo:getDouble("score")/2,
            ["status"]=jo:getString("status"),
            ["description"]=jo:getString("description"),
            ["thumbnail"]=jo:getObject("image"):getString("x225")
        }
    end
    return list
end

function getPages(url,chapter) -- table <Page>
    local array=JSONObject:create(network:load(url.."/chapter/"..chapter["id"])):getObject("response"):getObject("pages"):getArray("list")
    local pages={}
    for i=0,array:size()-1,1 do
        local jo=array:getObject(i)
        pages[i]={["page"]=jo:getInt("page"),["data"]=jo:getString("img")}
    end
    return pages
end

function load(file,data,url,cancel,process)
    return network:load(network:getClient(),data,domain,file,cancel,process)
end

function createAdvancedSearchOptions() -- table <Options>
    return {
        Options.new("Сортировка",utils:to_map(Sorts),0),
        Options.new("Жанры",utils:to_map(Genres),1)
    }
end

function loadSimilar(manga)
    local elements=network:load_as_Document(host.."/manga/"..manga["id"]):select("article.c-anime")
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

function status(status)
    return ({["released"]="Ongoing",["ongoing"]="Ongoing",["continued"]="Ongoing",["Заморожен"]="Stopped",["completed"]="Finished"})[status]
end