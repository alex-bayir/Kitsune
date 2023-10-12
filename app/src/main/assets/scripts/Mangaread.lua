---
--- Generated by Luanalysis
--- Created by Аlex Bayir.
--- DateTime: 03.03.2023 23:56
---

version="1.2"
domains={"www.mangaread.org"}
domain=domains[1]
source="MangaRead"
Type="Manga"
description="Один из лучших каталогов манги. Хорош тем, что на сайте быстро заливают новые главы."
host="https://"..domain
auth_tokens={}

Genres={["Action"]="action",["Adventure"]="adventure",["Animated"]="animated",["Anime"]="anime",["Cartoon"]="cartoon",["Comedy"]="comedy",["Comic"]="comic",["Completed"]="completed",["Cooking"]="cooking",["Detective"]="detective",["Doujinshi"]="doujinshi",["Drama"]="drama",["Ecchi"]="ecchi",["Fantasy"]="fantasy",["Gender-bender"]="gender-bender",["Harem"]="harem",["Historical"]="historical",["Horror"]="horror",["Isekai"]="isekai",["Josei"]="josei",["Magic"]="magic",["Manga"]="manga",["Manhua"]="manhua",["Manhwa"]="manhwa",["Martial-arts"]="martial-arts",["Mature"]="mature",["Mecha"]="mecha",["Military"]="military",["Mystery"]="mystery",["One-shot"]="one-shot",["Psychological"]="psychological",["Reincarnation"]="reincarnation",["Romance"]="romance",["School-life"]="school-life",["Sci-fi"]="sci-fi",["Seinen"]="seinen",["Shoujo"]="shoujo",["Shoujo-ai"]="shoujo-ai",["Shounen"]="shounen",["Shounen-ai"]="shounen-ai",["Slice-of-life"]="slice-of-life",["Smut"]="smut",["Sports"]="sports",["Super-power"]="super-power",["Supernatural"]="supernatural",["Thriller"]="thriller",["Tragedy"]="tragedy",["Webtoon"]="webtoon"}
Status={["Ongoing"]="on-going",["Completed"]="end",["Canceled"]="canceled",["On Hold"]="on-hold",["Upcoming"]="upcoming",}
Adult={["All"]="",["None adult"]="0",["Only Adult"]="1"}

function set_domain(new_domain)
    domain=new_domain
    host="https://"..domain
end

function update(url)
    url=url:gsub("https?:[\\/][\\/][^\\/]+",host)
    local e=network:load_as_Document(url):selectFirst("div.site-content")
    local list=e:select("li.wp-manga-chapter")
    local chapters={}; local last=list:size()-1
    for i=last,0,-1 do
        local elem=list:get(i); local href=elem:select("a"):attr("href")
        chapters[last-i]={vol=num(href:match("vol(%d+)")),num=num(href),date=utils:parseDate(elem:select("span"):text(),"dd.MM.yyyy"),id=href:match("chapter%-(.*)/")}
    end
    local author={}; local authors=e:select("a[href~=/m-author/]")
    for j=0,authors:size()-1,1 do
        local a=authors:get(j); author[a:attr("title")]=a:attr("abs:href")
    end
    return {
        ["url"]=url,
        ["url_web"]=url,
        ["name"]=e:selectFirst("h1"):text(),
        ["name_alt"]=e:select("div.summary-content"):get(2):text(),
        ["authors"]=author,
        ["genres"]=e:select("div.genres-content"):text(),
        ["rating"]=num(e:selectFirst("span.score"):text()),
        ["status"]=status(e:select("div.post-status"):select("div.summary-content"):text():match("[%d%s]*([%w%s]+)")),
        ["description"]=e:select("div.description-summary"):select("p[style=text-align: left;]"):text(),
        ["thumbnail"]=e:selectFirst("img.img-responsive"):attr("data-src"),
        ["chapters"]=chapters,
        ["similar"]=similar(e:select("div.related-reading-wrap"))
    }
end

function query(name,page,params)
    local url=network:url_builder(host..(page and "/page/"..(page+1) or "").."/"):add("s",name or ""):add("post_type","wp-manga")
    if(params~=nil and #params>0) then
        if(type(params[1])=="userdata" and Options:equals(params[1]:getClass())) then
            url:add("genre[]",params[1]:getSelected()):add("op",1)
            url:add("status[]",params[2]:getSelected())
            url:add("adult",params[3]:getSelected()[1])
            url:add("authors",params[4]:getInput())
            url:add("artist",params[5]:getInput())
            url:add("release",params[6]:getInput())
        else

        end
    end
    url=url:build()
    return query_url(url)
end

function query_url(url,page)
    if page then url=url:find("page/") and url:gsub("page/%d+","page/"..tostring(page+1)) or host.."/page/"..(page+1).."/"..url:match("(%?.*)") end
    print(url)
    local selects=network:load_as_Document(url):select("div.row.c-tabs-item__content")
    local list={}
    for i=0,selects:size()-1,1 do
        local e=selects:get(i)
        local author={}; local authors=e:select("a[href~=/m-author/]")
        for j=0,authors:size()-1,1 do
            local a=authors:get(j); author[a:attr("title")]=a:attr("abs:href")
        end
        list[i]={
            ["url"]=e:selectFirst("a"):attr("abs:href"),
            ["url_web"]=e:selectFirst("a"):attr("abs:href"),
            ["name"]=utils:text(e:selectFirst("h3.h4")),
            ["name_alt"]=utils:text(e:select("div.mg_alternative"):select("div.summary-content")),
            ["authors"]=author,
            ["genres"]=utils:text(e:select("div.mg_genres"):select("div.summary-content"),""):gsub(" ",", "),
            ["rating"]=num(utils:text(e:selectFirst("span.score"))),
            ["status"]=utils:text(e:select("div.mg_status"):select("div.summary-content")),
            ["thumbnail"]=e:selectFirst("img[class*=lazyload]"):attr("data-src")
        }
    end
    return list
end

function getPages(url,chapter)
    local elements=network:load_as_Document(url.."chapter-"..chapter["id"]):select("img.wp-manga-chapter-img")
    local pages={}
    for i=0,elements:size()-1,1 do
        pages[i]={["page"]=i+1,["data"]=elements:get(i):attr("data-src"):match("(http.*)")}
    end
    return pages
end

function load(file,data,url,cancel,process)
    return network:load(network:getClient(),data,domain,file,cancel,process)
end

function createAdvancedSearchOptions()
    return {
        {mode=1,title="Жанры",values=Genres},
        {mode=1,title="Статусы",values=Status},
        {mode=0,title="Взрослый контент",values=Adult},
        {mode=5,title="Автор"},
        {mode=5,title="Художник"},
        {mode=5,title="Год выпуска"}
    }
end

function similar(elements)
    local similar={}; local n=0
    for i=0,(elements~=nil and elements:size() or 0)-1,1 do
        local e=elements:get(i):selectFirst("a")
        local img=e and e:selectFirst("img")
        similar[n]=img and {
            ["url"]=e:attr("abs:href"),
            ["url_web"]=e:attr("abs:href"),
            ["name"]=e:attr("title"),
            ["name_alt"]=e:attr("title"),
            ["thumbnail"]=img:attr("data-src")
        }
        n=n+(img and 1 or 0)
    end
    return similar
end

function status(status)
    return ({["Upcoming"]="Announce",["OnGoing"]="Ongoing",["On Hold"]="Paused",["Canceled"]="Stopped",["Completed"]="Finished"})[status]
end