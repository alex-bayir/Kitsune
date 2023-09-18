---
--- Generated by Luanalysis
--- Created by Аlex Bayir.
--- DateTime: 03.04.2023 17:33
---

version="1.1"
domains={"mangareader.to"}
domain=domains[1]
source="MangaReaderTo"
Type="Manga"
description="Один из источников манги на которые раньше всего заливают главы."
host="https://"..domain
auth_tokens={}

Genres={["Action"]="1",["Adventure"]="2",["Cars"]="3",["Comedy"]="4",["Dementia"]="5",["Demons"]="6",["Doujinshi"]="7",["Drama"]="8",["Ecchi"]="9",["Fantasy"]="10",["Game"]="11",["Gender Bender"]="12",["Harem"]="13",["Hentai"]="14",["Historical"]="15",["Horror"]="16",["Josei"]="17",["Kids"]="18",["Magic"]="19",["Martial Arts"]="20",["Mecha"]="21",["Military"]="22",["Music"]="23",["Mystery"]="24",["Parody"]="25",["Police"]="26",["Psychological"]="27",["Romance"]="28",["Samurai"]="29",["School"]="30",["Sci-Fi"]="31",["Seinen"]="32",["Shoujo"]="33",["Shoujo Ai"]="34",["Shounen"]="35",["Shounen Ai"]="36",["Slice of Life"]="37",["Space"]="38",["Sports"]="39",["Super Power"]="40",["Supernatural"]="41",["Thriller"]="42",["Vampire"]="43",["Yaoi"]="44",["Yuri"]="45"}
Types={["All"]="",["Manga"]="1",["One-shot"]="2",["Doujinshi"]="3",["Light Novel"]="4",["Manhwa"]="5",["Manhua"]="6",["Comic"]="8"}
Status={["All"]="",["Finished"]="1",["Publishing"]="2",["On Hiatus"]="3",["Discontinued"]="4",["Not yet published"]="5"}
RatingType={["All"]="",["G - All Ages"]="1",["PG - Children"]="2",["PG-13 - Teens 13 or older"]="3",["R - 17+ (violence & profanity)"]="4",["R+ - Mild Nudity"]="5",["Rx - Hentai"]="6"}
Score={["All"]="",["(1) Appalling"]="1",["(2) Horrible"]="2",["(3) Very Bad"]="3",["(4) Bad"]="4",["(5) Average"]="5",["(6) Fine"]="6",["(7) Good"]="7",["(8) Very Good"]="8",["(9) Great"]="9",["(10) Masterpiece"]="10",}
Language={["All"]="",["English"]="en",["Japanese"]="ja",["Korean"]="ko",["Chinese"]="zh"}
Sorts={["Default"]="default",["Latest Updated"]="latest-updated",["Score"]="score",["Name A-Z"]="name-az",["Release Date"]="release-date",["Most Viewed"]="most-viewed"}

function set_domain(new_domain)
    domain=new_domain
    host="https://"..domain
end

function update(url)
    url=url:gsub("https?:[\\/][\\/][^\\/]+",host)
    local doc=network:load_as_Document(url)
    local container=doc:select("div.container")
    local genres=container:select("div.genres"):select("a[href*=/genre/]") local str="" for i=0,genres:size()-1,1 do str=str..", "..genres:get(i):text() end genres=str:sub(3)
    local ls=doc:select("ul.lang-chapters")
    local langs={}
    for i=0,ls:size()-1,1 do
        local lang=ls:get(i)
        langs[lang:id():match("..")]=lang:select("li.chapter-item")
    end
    local lang=langs["en"] and "en" or (langs["ja"] and "ja" or ls:get(0):id())
    local list=langs[lang]
    local chapters={}; local last=list:size()-1
    for i=last,0,-1 do
        local e=list:get(i);
        chapters[last-i]=Chapter.new(0,num(e:attr("data-number")), e:select("a[title]"):attr("title"):match("Chapter %d*%.?%d+: (.*)"),0,utils:to_map({["lang"]=lang}))
    end

    local author={}; local authors=container:select("div.anisc-info"):select("a[href*=/author/]")
    for j=0,authors:size()-1,1 do
        local a=authors:get(j); author[a:text()]=a:attr("abs:href")
    end
    local status=container:select("div.item"):text():match("Status: (%S+)")
    return {
        ["url"]=url,
        ["url_web"]=url,
        ["name"]=container:select("h2.manga-name"):text(),
        ["name_alt"]=container:select("div.manga-name-or"):text(),
        ["author"]=author,
        ["genres"]=genres,
        ["status"]=status:find("Finished") and "2" or (status:find("Publishing") and "1" or "0"),
        ["rating"]=num(container:select("div.item"):text():match("Score: (%d*%.?%d+)"))/2,
        ["description"]=container:select("div.description"):text(),
        ["thumbnail"]=container:select("img.manga-poster-img"):attr("src"),
        ["chapters"]=chapters,
        ["similar"]=similar(doc:select("li.item-top"))
    }
end

function query(name,page,params)
    local url
    if name then
        url=network:url_builder(host.."/search"):add("keyword",name):add("page",page+1)
    else
        url=network:url_builder(host.."/filter"):add("page",page+1)
        if(params~=nil and #params>0) then
            if(type(params[1])=="userdata" and Options:equals(params[1]:getClass())) then
                url:add("genres",params[1]:getSelected(),",")
                if(#params>1) then url:add("type",params[2]:getSelected()) end
                if(#params>2) then url:add("score",params[3]:getSelected()) end
                if(#params>3) then url:add("language",params[4]:getSelected()) end
                if(#params>4) then url:add("sort",params[5]:getSelected()) end
                if(#params>5) then url:add("status",params[6]:getSelected()) end
                if(#params>6) then url:add("rating_type",params[7]:getSelected()) end
            else
                local switch={[0]="most-viewed",[1]="release-date",[2]="latest-updated"}
                url:add("sort",switch[params[1]])
            end
        end
    end
    url=url:build()
    if(name~=nil and name:match("[a-z]://[^ >,;]*")~=nil) then url=name; end
    return query_url(url)
end

function query_url(url,page)
    if page then url=url:find("page=") and url:gsub("page=%d+","page="..tostring(page+1)) or url.."&page="..tostring(page+1) end
    print(url)
    local selects=network:load_as_Document(url):select("div.item-spc")
    local list={}
    for i=0,selects:size()-1,1 do
        local e=selects:get(i)
        list[i]={
            ["url"]=e:selectFirst("a"):attr("abs:href"),
            ["url_web"]=e:selectFirst("a"):attr("abs:href"),
            ["name"]=utils:text(e:selectFirst("h3")),
            ["genres"]=utils:text(e:select("span.fdi-cate")),
            ["thumbnail"]=e:selectFirst("img"):attr("src")
        }
    end
    return list
end

function getPages(url,chapter) -- table <Page>
    local info=network:load_as_Document(url:gsub(host,host.."/read").."/"..chapter["lang"].."/chapter-"..chapter["num"]):select("div#wrapper")
    local elements=network:parse(
            JSONObject:create(
                    network:load(host.."/ajax/image/list/"..info:attr("data-reading-by").."/"..info:attr("data-reading-id"))
            ):get("html")
    ):select("div.iv-card")
    local pages={}
    for i=0,elements:size()-1,1 do
        pages[i]={["page"]=i+1,["data"]=elements:get(i):attr("data-url")}
    end
    return pages
end

function load(file,data,url,cancel,process)
    return network:load(network:getClient(true),data,domain,file,cancel,process)
end

function createAdvancedSearchOptions() -- table <Options>
    return {
        Options.new("Жанры",utils:to_map(Genres),1),
        Options.new("Тип",utils:to_map(Types),0),
        Options.new("Рейтинг",utils:to_map(Score),0),
        Options.new("Язык",utils:to_map(Language),0),
        Options.new("Сортировка",utils:to_map(Sorts),0),
        Options.new("Статус",utils:to_map(Status),0),
        Options.new("Возрастной Рейтинг",utils:to_map(RatingType),0)
    }
end

function similar(elements)
    local similar={}
    for i=0,(elements and elements:size() or 0)-1,1 do
        local e=elements:get(i)
        similar[i]={
            ["url"]=e:select("a"):attr("abs:href"),
            ["url_web"]=e:select("a"):attr("abs:href"),
            ["name"]=e:select("a[title]"):attr("title"),
            ["thumbnail"]=e:select("img"):attr("src")
        }
    end
    return similar
end