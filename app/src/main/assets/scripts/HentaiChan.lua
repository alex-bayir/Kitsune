---
--- Generated by Luanalysis
--- Created by А.
--- DateTime: 22.03.2022 13:49
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

version="1.2"
provider="x.hentaichan.live"
providerName="HentaiChan"
sourceDescription="Самый известный каталог хентая. Пожалуйста отключите этот источник если вам меньше 18."
host="https://"..provider
auth_tokens={"PHPSESSID"}

Genres={["3D"]="3D",["action"]="action",["ahegao"]="ahegao",["bdsm"]="bdsm",["corruption"]="corruption",["foot fetish"]="foot_fetish",["footfuck"]="footfuck",["gender bender"]="gender_bender",["live"]="live",["lolcon"]="lolcon",["megane"]="megane",["mind break"]="mind_break",["monstergirl"]="monstergirl",["netorare"]="netorare",["netori"]="netori",["nipple penetration"]="nipple_penetration",["oyakodon"]="oyakodon",["paizuri (titsfuck)"]="paizuri_(titsfuck)",["rpg"]="rpg",["scat"]="scat",["shemale"]="shemale",["shooter"]="shooter",["simulation"]="simulation",["skinsuit"]="skinsuit",["tomboy"]="tomboy",["x-ray"]="x-ray",["алкоголь"]="алкоголь",["анал"]="анал",["андроид"]="андроид",["анилингус"]="анилингус",["анимация"]="анимация",["аркада"]="аркада",["арт"]="арт",["бабушка"]="бабушка",["без текста"]="без_текста",["без трусиков"]="без_трусиков",["без цензуры"]="без_цензуры",["беременность"]="беременность",["бикини"]="бикини",["близнецы"]="близнецы",["боди-арт"]="боди-арт",["больница"]="больница",["большая грудь"]="большая_грудь",["большие попки"]="большие_попки",["бондаж"]="бондаж",["буккаке"]="буккаке",["в ванной"]="в_ванной",["в общественном месте"]="в_общественном_месте",["в первый раз"]="в_первый_раз",["в цвете"]="в_цвете",["в школе"]="в_школе",["вампиры"]="вампиры",["веб"]="веб",["вебкам"]="вебкам",["вибратор"]="вибратор",["визуальная новелла"]="визуальная_новелла",["внучка"]="внучка",["волосатые женщины"]="волосатые_женщины",["гаремник"]="гаремник",["гг девушка"]="гг_девушка",["гг парень"]="гг_парень",["гипноз"]="гипноз",["глубокий минет"]="глубокий_минет",["горничные"]="горничные",["горячий источник"]="горячий_источник",["грудастая лоли"]="грудастая_лоли",["групповой секс"]="групповой_секс",["гяру и гангуро"]="гяру_и_гангуро",["двойное проникновение"]="двойное_проникновение",["девочки волшебницы"]="девочки_волшебницы",["девушка туалет"]="девушка_туалет",["демоны"]="демоны",["дилдо"]="дилдо",["дочь"]="дочь",["драма"]="драма",["дыра в стене"]="дыра_в_стене",["жестокость"]="жестокость",["за деньги"]="за_деньги",["зомби"]="зомби",["зрелые женщины"]="зрелые_женщины",["измена"]="измена",["изнасилование"]="изнасилование",["инопланетяне"]="инопланетяне",["инцест"]="инцест",["исполнение желаний"]="исполнение_желаний",["камера"]="камера",["квест"]="квест",["кимоно"]="кимоно",["колготки"]="колготки",["комиксы"]="комиксы",["косплей"]="косплей",["кремпай"]="кремпай",["кудере"]="кудере",["кузина"]="кузина",["куннилингус"]="куннилингус",["купальники"]="купальники",["латекс и кожа"]="латекс_и_кожа",["магия"]="магия",["маленькая грудь"]="маленькая_грудь",["мастурбация"]="мастурбация",["мать"]="мать",["мейдочки"]="мейдочки",["мерзкий дядька"]="мерзкий_дядька",["минет"]="минет",["много девушек"]="много_девушек",["молоко"]="молоко",["монашки"]="монашки",["монстры"]="монстры",["мочеиспускание"]="мочеиспускание",["мужская озвучка"]="мужская_озвучка",["мужчина крепкого телосложения"]="мужчина_крепкого_телосложения",["мускулистые женщины"]="мускулистые_женщины",["на природе"]="на_природе",["наблюдение"]="наблюдение",["непрямой инцест"]="непрямой_инцест",["новелла"]="новелла",["обмен партнерами"]="обмен_партнерами",["обмен телами"]="обмен_телами",["обычный секс"]="обычный_секс",["огромная грудь"]="огромная_грудь",["огромный член"]="огромный_член",["остановка времени"]="остановка_времени",["парень пассив"]="парень_пассив",["переодевание"]="переодевание",["песочница"]="песочница",["племянница"]="племянница",["пляж"]="пляж",["подглядывание"]="подглядывание",["подчинение"]="подчинение",["похищение"]="похищение",["презерватив"]="презерватив",["принуждение"]="принуждение",["прозрачная одежда"]="прозрачная_одежда",["проникновение в матку"]="проникновение_в_матку",["психические отклонения"]="психические_отклонения",["публично"]="публично",["рабыни"]="рабыни",["романтика"]="романтика",["сверхъестественное"]="сверхъестественное",["секс игрушки"]="секс_игрушки",["сестра"]="сестра",["сетакон"]="сетакон",["скрытный секс"]="скрытный_секс",["спортивная форма"]="спортивная_форма",["спящие"]="спящие",["страпон"]="страпон",["суккубы"]="суккубы",["темнокожие"]="темнокожие",["тентакли"]="тентакли",["толстушки"]="толстушки",["трап"]="трап",["тётя"]="тётя",["умеренная жестокость"]="умеренная_жестокость",["учитель и ученик"]="учитель_и_ученик",["ушастые"]="ушастые",["фантазии"]="фантазии",["фантастика"]="фантастика",["фемдом"]="фемдом",["фестиваль"]="фестиваль",["фистинг"]="фистинг",["фурри"]="фурри",["футанари"]="футанари",["футанари имеет парня"]="футанари_имеет_парня",["фэнтези"]="фэнтези",["хоррор"]="хоррор",["цундере"]="цундере",["чикан"]="чикан",["чирлидеры"]="чирлидеры",["чулки"]="чулки",["школьная форма"]="школьная_форма",["школьники"]="школьники",["школьницы"]="школьницы",["школьный купальник"]="школьный_купальник",["щекотка"]="щекотка",["эксгибиционизм"]="эксгибиционизм",["эльфы"]="эльфы",["эччи"]="эччи",["юмор"]="юмор",["юри"]="юри",["яндере"]="яндере",["яой"]="яой"}
Sorts={["Популярность"]="fav",["Дата"]="date",["Алфавит"]="abc"}
sorts={[1]="fav",[2]="date",[3]=nil}
Order={["По убыванию"]="desc",["По возрастанию"]="asc"}

function update(url) -- Wrapper
    local e=Wrapper:loadDocument(url):body():selectFirst("div.main_fon")
    local list=Wrapper:loadDocument(url:gsub("manga","online")):select("select#related"):select("option")
    local chapters=ArrayList.new(list:size())
    for i=0,list:size()-1,1 do
        local el=list:get(i)
        local str=el:attr("value")
        if(str:match("-.*-")==url:match("-.*-")) then
            chapters:add(Chapter.new(num(str:match("%d+")),0,num(str:match("(%d*%.?%d+)[%D]*$")),nil,0))
        end
    end
    if(chapters:size()==0) then chapters:add(Chapter.new(num(url:match("%d+")),0,0,"Сингл",0)) end
    return Wrapper.new(
            url,
            num(Wrapper:attr(e:selectFirst("a.title_top_a"),"href"):match("%d+")),
            Wrapper:text(e:selectFirst("a.title_top_a"),""):match("[a-zA-Z].*[a-zA-Z]"),
            Wrapper:text(e:selectFirst("a.title_top_a"),""):match("[а-яА-Я].*[а-яА-Я]"),
            Wrapper:text(e:select("div.row"):get(1):selectFirst("a")),
            Wrapper:attr(e:select("div.row"):get(1):selectFirst("a"),"abs:href"),
            Wrapper:text(e:select("li.sidetag"),""):gsub("%s?%+%s%-%s",", "):sub(3),
            0,
            0,
            Wrapper:text(e:selectFirst("div#description")),
            Wrapper:attr(e:selectFirst("img#cover"),"src"),
            url,
            chapters
    )
end
function query(name,page,params) -- java.util.ArrayList<Wrapper>
    local url;
    if(name~=nil and name:len()>0) then
        url=UrlBuilder.new(host.."/"):addParam("do","search"):addParam("subaction","search"):addParam("story",name):addParam("offset",page>0 and (page+1)*10 or nil):getUrl();
    else
        if(params~=nil and #params>0 and type(params[1])=="userdata" and Options:equals(params[1]:getClass()))then
            if(params~=nil and #params>0) then
                url=host.."/manga/new&n="..params[1]:getSelected()[1]..params[1]:getTitleSortSelected()
                s=params[2]~=nil and params[2]:getSelected() or nil
                d=params[2]~=nil and params[2]:getDeselected() or nil
                if((s~=nil and #s>0) or (d~=nil and #d>0)) then
                    url=host.."/tags/"; local b=false
                    for i=1,(s~=nil and #s or 0),1 do
                        url=url..(b and "+" or "")..s[i]; b=true
                    end
                    for i=1,(d~=nil and #d or 0),1 do
                        url=url..(b and "+-" or "")..d[i]; b=true
                    end
                    url=url.."&sort=manga".."&n="..params[1]:getSelected()[1]..params[1]:getTitleSortSelected()
                end
            end
        elseif(params~=nil and #params>0)then
            url=sorts[params[1]]~=nil and host.."/manga/new&n="..sorts[params[1]] or host.."/manga/random"
        end
        url=url~=nil and url..(page>0 and ("?offset="..((page+1)*10)) or "")
    end
    if(name~=nil and name:match("[a-z]://[^ >,;]*")~=nil) then url=name; end
    print(url)
    local selects=Wrapper:loadDocument(url):select("div.content_row")
    local list=ArrayList.new(selects:size())
    for i=0,selects:size()-1,1 do
        local e=selects:get(i)
        if(Wrapper:attr(e:selectFirst("h2"):selectFirst("a"),"href"):match("/manga.*")) then
            list:add(Wrapper.new(
                    Wrapper:attr(e:selectFirst("h2"):selectFirst("a"),"abs:href"),
                    0,
                    Wrapper:text(e:selectFirst("h2"),""):match("[a-zA-Z].*[a-zA-Z]"),
                    Wrapper:text(e:selectFirst("h2"),""):match("[а-яА-Я].*[а-яА-Я]"),
                    Wrapper:text(e:selectFirst("div.manga_row3") and e:selectFirst("div.manga_row3"):selectFirst("a")),
                    Wrapper:attr(e:selectFirst("div.manga_row3") and e:selectFirst("div.manga_row3"):selectFirst("a"),"abs:href"),
                    Wrapper:text(e:select("div.genre"),""):gsub("_"," "),
                    0,
                    0,
                    Wrapper:text(e:select("div.tags")),
                    (select(1, Wrapper:attr(e:selectFirst("img"),"src"):gsub("_blur","")))
            ))
        end
    end
    return list
end
function getPages(url,chapter) -- ArrayList<Page>
    local array=JSONArray.new(Wrapper:loadDocument(host.."/online/"..chapter.id..url:match("%d+(.*)%d*%.")..chapter.num..".html"):select("script"):toString():match("\"fullimg\":%s*(%[.-%])"):gsub("'","\""))
    local pages=ArrayList.new(array:length())
    for i=0,array:length()-1,1 do
        pages:add(Page.new(0,i+1,array:getString(i)))
    end
    return pages
end
function createAdvancedSearchOptions() --ArrayList<Options>
    local options=ArrayList.new()
    options:add(Options.new("Сортировка","desc","asc",convert(Sorts),0))
    options:add(Options.new("Жанры",convert(Genres),2))
    return options
end

function num(n) return n==nil and 0 or tonumber(n:match("[0-9]*%.?[0-9]+")) end

function convert(luaTable)
    local javaTable=Map_class.new()
    for key,value in pairs(luaTable) do javaTable:put(key,value) end
    return javaTable
end

function loadSimilar(wrapper)
    local elements=Wrapper:loadDocument(wrapper.url:gsub("manga","related")):select("div.related")
    local similar=ArrayList.new(elements:size())
    for i=0,(elements~=nil and elements:size() or 0)-1,1 do
        local e=elements:get(i)
        if(e~=nil) then
            similar:add(Wrapper.new(
                    Wrapper:attr(e:selectFirst("h2"):selectFirst("a"),"abs:href"),
                    0,
                    Wrapper:text(e:selectFirst("h2"),""):match("[a-zA-Z].*[a-zA-Z]"),
                    Wrapper:text(e:selectFirst("h2"),""):match("[а-яА-Я].*[а-яА-Я]"),
                    Wrapper:text(e:select("div.related_row"):get(1):selectFirst("a")),
                    Wrapper:attr(e:select("div.related_row"):get(1):selectFirst("a"),"abs:href"),
                    Wrapper:text(e:select("div.genre")),
                    0,
                    0,
                    Wrapper:text(e:select("div.tags")),
                    host..((select(1,Wrapper:attr(e:selectFirst("img"),"src"):match("/manga.*"):gsub("_blur",""))))
            ))
        end
    end
    return similar
end