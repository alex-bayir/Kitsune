import com.alex.json.java.JSON;
import org.alex.kitsune.book.Book;
import org.alex.kitsune.book.Book_Scripted;
import org.alex.kitsune.book.Chapter;
import org.alex.kitsune.scripts.Script;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.alex.kitsune.book.search.FilterSortAdapter;
import org.alex.kitsune.utils.NetworkUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.testng.annotations.Test;

public class ScriptsTests {
    public void print(String str){System.out.print(str);}
    public void println(String str){System.out.println(str);}
    public void print(Object obj){print(obj!=null?obj.toString():"null");}
    public void println(Object obj){println(obj!=null?obj.toString():"null");}
    @Test
    public void tmp() throws Throwable {
        //println(Wrapper.loadDocument("https://vk.com/org.alex.kitsune").select("a.page_doc_title").toString());
        /*
        String tmp=Utils.group(NetworkUtils.getDocument("https://v1.hentailib.org/manga-list").select("script").toString(),"window.__DATA = (\\{.*\\});");
        JSON.Object json=JSON.Object.create(tmp).getObject("filters");
        json.forEach((k,v)->{
            println(k); String z="";
            List<JSON.Object> array=JSON.filter(json.getArray(k),JSON.Object.class);
            if(array!=null){
                for(JSON.Object obj:array){
                    print(z+"[\""+capitalize(obj.get("name",obj.getString("label"))).replace("\\","\\\\")+"\"]=\""+obj.getInt("id")+"\"");
                    z=",";
                }
            }
            println("");
        });
         */
        Elements elements=NetworkUtils.getDocument("https://mangareader.to/filter").select("div.f-genre-item");//Wrapper.loadDocument("https://manga-chan.me/catalog").select("li.sidetag");
        //Elements elements= Jsoup.parse(tmp).select("li");
        println("");
        println(elements.size());
        for(Element e:elements){
            //print(e.selectFirst("a").selectFirst("div.strong.title").attr("title"));
            //print(",[\""+capitalize(e.text())+"\"]=\""+e.selectFirst("input").attr("id")+"\"");
            print(",[\""+e.text()+"\"]=\""+e.attr("data-id")+"\"");
        }
        println("");
    }
    public static String capitalize(String str){
        return str==null? null : str.substring(0,1).toUpperCase()+str.substring(1);
    }
    @Test
    public void f1() throws Throwable {
        Script script=Script.getInstance(new File("C:\\Games\\Kitsune\\app\\src\\main\\assets\\scripts\\ReadManga.lua"));
        List<Book> list=Book_Scripted.query(script,"ванпанчмен",0);
        Book book=list.get(0);
        book.update();
        book.loadSimilar(obj -> {}, obj -> {});
        Chapter chapter= book.getChapters().get(0);
        book.getPages(chapter);
        println("end");
    }
    @Test
    public void f2() throws Throwable {
        Script script=Script.getInstance(new File("C:\\Games\\Kitsune\\app\\src\\main\\assets\\scripts\\HentaiChan.lua"));
        FilterSortAdapter adapter= Book_Scripted.createAdvancedSearchAdapter(script);
        List<Book> list=Book_Scripted.query(script,"hello",0,(Object[])adapter.getOptions());
        Book book=list.get(1);
        book.update();
        println("end");
    }
    @Test
    public void syntax() throws Throwable {
        //new Lua(new File("C:\\Games\\Kitsune\\luatests\\script.lua")).invokeMethod("tmp");
        println(String.format("%d",10));
        String text="Kitsune_1.5.6.apk0";//Utils.File.readFile(new File("C:\\Games\\Kitsune\\app\\src\\main\\assets\\scripts\\Remanga.lua"));
        text="";
        Pattern pattern=Pattern.compile("".replaceAll("[\\\\\\.\\*\\+\\-\\?\\^\\$\\|\\(\\)\\[\\]\\{\\}]","\\\\W"));
        println("pattern=\""+pattern.pattern()+"\"");
        Matcher matcher=pattern.matcher(text);
        while(matcher.find()){
            println(text.substring(matcher.start(), matcher.end()));
        }
    }
    @Test
    public void json() throws Throwable {
        //println(Utils.unescape_unicodes("uu0027u\\0027\\\\0027\\\"\\t\\n\\tstring\\\"\n\t u0423 \u043d\\0435u0451 \\u043du0435 u0432u0441u0451 u0432 u043fu043eu0440u044fu0434u043au0435 u0441 u0433u043eu043bu043eu0432u043eu0439 - u0440u0435u0436u0438u0441u0441u0451u0440u0441u043au0430u044f u0432u0435u0440u0441u0438u044f"));
        JSON.Object obj=new JSON.Object()
                .put("tmp",null)
                .put("l",System.currentTimeMillis())
                .put("d",3.2)
                .put("d2",3.0)
                .put("string","\"\t\n\tstring\"")
                .put("url","https://www.manga.org")
                .put("arr",new int[]{1,2,3})
                .put("arr2",new Object[]{"1","2",3.1,"4sdfds",null,new int[]{5,6,7}});
        String json=obj.json(1);//"{\"msg\":\"\",\"content\":{\"id\":2912,\"img\":{\"high\":\"/media/titles/crawling-dreams/high_cover.jpg\",\"mid\":\"/media/titles/crawling-dreams/mid_cover.jpg\",\"low\":\"/media/titles/crawling-dreams/low_cover.jpg\"},\"en_name\":\"Crawling Dreams\",\"rus_name\":\"Ползущие сны\",\"another_name\":\"\",\"dir\":\"crawling-dreams\",\"description\":\"<p>Нярла и Гаст &mdash; друзья, живущие в огромном модном городе, расположенном у моря. Однако, похоже, переулки этого города скрывают совсем неожиданные секреты и тайны.</p>\",\"issue_year\":2017,\"avg_rating\":\"8.4\",\"admin_rating\":\"0.0\",\"count_rating\":22,\"age_limit\":0,\"status\":{\"id\":1,\"name\":\"Продолжается\"},\"count_bookmarks\":438,\"total_votes\":3826,\"total_views\":21786,\"type\":{\"id\":0,\"name\":\"Манга\"},\"genres\":[{\"id\":16,\"name\":\"Элементы юмора\"},{\"id\":21,\"name\":\"Повседневность\"},{\"id\":27,\"name\":\"Сверхъестественное\"},{\"id\":36,\"name\":\"Ужасы\"}],\"categories\":[{\"id\":5,\"name\":\"Веб\"},{\"id\":6,\"name\":\"В цвете\"}],\"bookmark_type\":null,\"rated\":null,\"branches\":[{\"id\":2760,\"img\":\"/static/images/publishers/no-image.jpg\",\"subscribed\":false,\"total_votes\":3826,\"count_chapters\":63,\"publishers\":[{\"id\":766,\"name\":\"Stacey Nicks\",\"img\":\"/static/images/publishers/no-image.jpg\",\"dir\":\"stacey_nicks\",\"tagline\":\"\",\"type\":\"Переводчик\"}]}],\"count_chapters\":63,\"first_chapter\":{\"id\":146985,\"tome\":1,\"chapter\":\"0\"},\"continue_reading\":null,\"is_licensed\":false,\"newlate_id\":null,\"newlate_title\":null,\"related\":null,\"uploaded\":1,\"can_post_comments\":true,\"adaptation\":null,\"publishers\":[{\"id\":766,\"name\":\"Stacey Nicks\",\"img\":\"/static/images/publishers/no-image.jpg\",\"dir\":\"stacey_nicks\",\"tagline\":\"\",\"type\":\"Переводчик\"}],\"is_yaoi\":false,\"is_erotic\":false},\"props\":{\"age_limit\":[{\"id\":0,\"name\":\"Для всех\"},{\"id\":1,\"name\":\"16+\"},{\"id\":2,\"name\":\"18+\"}],\"can_upload_chapters\":false,\"can_update\":true,\"can_pin_comment\":false,\"promo_offer\":null,\"admin_link\":null,\"panel_link\":null}}\n";
        //json="[\"{\"count\":4,\"first\":false,\"name\":\"Saved\"}\",\"{\"count\":3,\"first\":true,\"name\":\"History\"}\",\"{\"count\":4,\"first\":false,\"name\":\"Favorite\"}\",\"{\"count\":4,\"first\":false,\"name\":\"Maybe\"}\",\"{\"count\":4,\"first\":false,\"name\":\"KonoSuba\"}\"]";
        println(json);
        JSON.Object tmp=JSON.Object.create(json);
        println(tmp.json(1));
        //tmp=((JSON.Object)tmp).get("content");
        //println(tmp.toString());
    }
    @Test
    public void ranobe() throws Throwable {
        Element content=Jsoup.parse("<div class=\"reader-container container container_center\"><p>— Добро пожаловать в загробную жизнь, Сато Казума-сан. К сожалению, ты умер. Быть может, твоя жизнь и была коротка, но ей пришёл конец.</p><p>В белоснежной комнате кто-то внезапно заговорил со мной.</p><p>Меня смутил неожиданный поворот событий.</p><p>В комнате был расположен комплект офисной мебели, включавший стол и кресло, и на этом кресле сидела та, что объявила, что моя жизнь окончилась.</p><p>Если бы богини существовали, то она была бы одной из них. Красота её намного превосходила красоты телевизионных идолов, она сияла так, как не могли сиять люди. У неё были длинные синие волосы, которые выглядели гладкими, как шёлк. Похоже, ей было столько же, сколько и мне. Её грудь не была слишком пышной или слишком скромной. Она носила лёгкий фиолетовый хагоромо*&nbsp;поверх остальной одежды.</p><p>Красавица моргнула; глаза её, что были такого же синего цвета морской волны, как её волосы, были обращены ко мне, а я не знал, что происходит.</p><p>...Я вспомнил то, что случилось чуть ранее.</p><p>...Выходить из дома для меня было редкостью.</p><p>Чтобы купить первый выпуск популярной онлайновой игры из ограниченного тиража, я встал рано, чтобы занять очередь.</p><p>Людей вроде меня в обществе называют хикикомори.</p><p>После покупки игры пришло время вернуться домой, чтобы вдоволь наиграться. У меня было прекрасное настроение, когда я об этом подумал и приготовился идти домой, но в тот момент...</p><p>Передо мной шла уставившаяся в свой телефон девушка. Если судить по её форме, она должна была учиться в той же школе, что и я.</p><p>Когда девушка увидела зелёный сигнал светофора, она пошла вперёд, не посмотрев по сторонам.</p><p>Большая тень надвигалась на неё. Должно быть, на огромной скорости к ней нёсся грузовик.</p><p>Когда я пришёл в себя, я оттолкнул ту девушку.</p><p>После этого...</p><p>...Я спросил красавицу передо мной с таинственно спокойной интонацией:</p><p>— ...Могу я задать один вопрос?</p><p>Красавица кивнула в ответ.</p><p>— Пожалуйста, задавай.</p><p>— ...Та девушка, которую я оттолкнул, выжила ли она?</p><p>Это было важнее всего. Это был первый и последний раз. Я сделал что-то стоящее. Было бы очень обидно, если бы моя попытка спасти её ценой своей жизни не увенчалась успехом.</p><p>— Она жива! Но у неё серьёзная травма: она сломала ногу.</p><p>Хвала небесам...</p><div class=\"article-image\"><img class=\"lazyload loaded\" data-background=\"\" data-src=\"https://ranobelib.me/uploads/ranobe/kono-subarashii-sekai-ni-shukufuku-wo/chapters/1-0.1/image_ExOs.png\" src=\"https://ranobelib.me/uploads/ranobe/kono-subarashii-sekai-ni-shukufuku-wo/chapters/1-0.1/image_ExOs.png\" data-was-processed=\"true\"></div><p>Я погиб не зря. Я сделал что-то хорошее в самом конце...</p><p>Увидев, что я вздохнул с облегчением, красавица наклонила голову и сказала:</p><p>— Но она бы не пострадала, если бы ты не толкнул её.</p><p>— ...А?</p><p>Что она сказала?</p><p>— Тот трактор остановился бы до того, как ударил бы ту девушку. Это было ожидаемо, ведь это был, в конце концов, медленный трактор. И это означает, что твоё ненужное вмешательство в попытке быть героем только сделало всё хуже... Пу-хи-хи!</p><p>Да что же это, я впервые встретил эту девушку. Возможно, это грубо, но я хотел поколотить её.</p><p>...Неправильно, подожди. Мне кажется, что я услышал нечто куда более важное, чем это.</p><p>— ...Что ты сказала? Трактор? Не грузовик?</p><p>— Верно, трактор. Если бы в сторону той девушки на полной скорости мчался грузовик, она бы заметила это и ушла бы с пути.</p><p>...Что?</p><p>— Э? Но что насчёт меня? Я погиб, после того как меня ударил трактор?</p><p>— Нет, ты умер от шока, потому что тебе показалось, что тебя задавил грузовик. Я долгое время занимаюсь этим, но ты первый, кто умер так неестественно!</p><p>...</p><p>— Из-за невероятно опасной ситуации с трактором ты потерял сознание и контроль над мочевым пузырём. Тебя отправили в ближайшую больницу. В то время как доктора и медсёстры, смеясь, говорили: \"Да что же с этим парнем, настолько никудышный-- (лол)\", ты так и не пришёл в сознание, а твоё сердце остановилось...</p><p>— Замолкни--! Не хочу об этом слышать! Не хочу слушать такие отвратные вещи!</p><p>Когда я закрыл свои уши руками, девушка приблизилась ко мне сбоку и подло ухмыльнулась, придвинувшись ближе:</p><p>— Твоя семья прибыла в больницу, но перед тем как они смогли ощутить скорбь от своей утраты, они разразились хохотом, услышав причину твоей смерти, и ничего не могли с этим поделать.</p><p>— Заткнись, заткнись! Это не может быть правдой! Как вообще может существовать такой никудышный способ умереть, просто невероятно!</p><p>Смотря на меня сверху вниз, пока я сидел, закрыв голову руками, девушка рассмеялась, прикрыв рот ладонью.</p><p>— ...Ладно, вот и конец моему расслабляющему сеансу. Это наша первая встреча, Сато Казума-сан. Меня зовут Аква. Я богиня, которая проводит молодых людей, умерших в Японии... Теперь, если отбросить в сторону то, как забавно ты умер, у тебя есть два варианта.</p><p>...Эта девка!</p><p>Забудь, если оставаться взвинченным, это лишь замедлит прогресс разговора, мне просто нужно потерпеть.</p><p>— Во-первых, можно перевоплотиться и начать новую жизнь. Или можно остаться в месте наподобие рая и вести новую жизнь как в доме престарелых.</p><p>Какое ленивое описание возможностей.</p><p>— Э, ну... Что это за место вроде рая? Что важнее, что ты подразумеваешь под домом престарелых?</p><p>— Рай не настолько хорош, как воображают люди. После смерти тебе не потребуется пища, и ты не сможешь делать что-либо связанное с естественными потребностями. Да и в любом случае для тебя там не будет никаких вещей, которые бы тебе потребовались. Прости, если разочаровываю тебя, но в раю ничего нет: ни телевизора, ни манги, ни игр. Там есть только другие люди, которые умерли до тебя. И, так как ты умер, ты не сможешь сделать что-нибудь извращённое, потому что у тебя даже нет тела. Ты сможешь только согреваться в лучах солнца со своими предшественниками и болтать. И так навечно.</p><p>Без компьютерных игр и развлечений это был скорее ад, чем рай. Но стать младенцем и заново начать свою жизнь... Нет, это был единственный путь.</p><p>Глядя на моё огорчённое лицо, богиня улыбнулась и сказала:</p><p>— Эй, ты же не хочешь отправиться в такое скучное подобие рая? Но просить тебя откинуть все твои старые воспоминания и начать с самого начала младенцем — это будто стереть твоё существование, потому что твоя память будет утеряна. И вот! У меня для тебя отличная новость!</p><p>По какой-то причине я был настроен абсолютно скептически.</p><p>— Тебе нравятся... игры? — с улыбкой спросила настороженного меня Аква.</p><p>Аква спокойно поведала свою так называемую отличную новость.</p><p>Суть её заключалась вот в чём. В мире, отличном от того, где жил я, был Король Демонов, войска которого ввергнули мир в кризис.</p><p>В том мире существовали магия и монстры. Проще говоря, это был фэнтезийный мир, имеющий сходство с известными играми вроде Dragon Quest и Final Fantasy.</p><p>— Люди, которые погибают в том мире от рук прихвостней Короля Демонов, очень напуганы и говорят, что не хотят снова так умирать. И поэтому почти все умершие в том мире отказываются возрождаться там снова. Если говорить конкретнее, тому миру придёт конец, если так будет продолжаться, ведь там прекратят рождаться дети. Так что отправка туда умерших из других миров решит эту проблему, верно? Вот так.</p><p>Какой небрежный подход к иммиграции.</p><p>— Так как мы пересылаем туда людей, мы должны найти тех, кто умер в молодом возрасте и всё ещё хочет жить, и должны отправить их вместе с их исходными телами и воспоминаниями. Было бы глупо, если бы они сразу погибали после перехода, поэтому мы предоставляем им исключительное право взять с собой в тот мир одну вещь, которая им понравится. Это может быть мощная способность, необыкновенный талант или оружие божественного уровня... Что думаешь? Пусть это и другой мир, но у тебя будет ещё одна попытка прожить свою жизнь. А для людей в том мире появится кто-то, кто сразу сможет вступить в бой. Ну как? Разве это не отличная новость?</p><p>Ясно, звучит неплохо.</p><p>Если честно, это заставило меня ощутить волнение. Мне нравились компьютерные игры, но я никогда бы не подумал, что смогу попасть в мир, который будет похож на любимые из них.</p><p>Но перед этим:</p><p>— Эм, у меня вопрос. Что насчёт языка, на котором говорят в том мире? Смогу ли я говорить на языке другого мира?</p><p>— Это не проблема. За счёт любезной божественной помощи твой мозг напрямую выучит язык в тот миг, когда ты попадёшь в другой мир. Ты даже сможешь читать! Но есть побочный эффект: если тебе не повезёт, содержимое твоего мозга может целиком и полностью быть стёрто... Так что тебе остаётся только выбрать крутую способность или оружие.</p><p>— Подожди, я только что услышал нечто важное. Ты сказала, что содержимое моего мозга может быть стёрто, если мне не повезёт?</p><p>— Я этого не говорила.</p><p>— Нет, сказала.</p><p>Недавняя напряжённость куда-то пропала. Я разговаривал с богиней, но при этом держался так, будто мы были равны.</p><p>...Но это было заманчивое предложение. Возможность остаться с затёртым подчистую мозгом пугала, но если сказать, что я с ранних лет уверен в своей везучести, то это не будет простым хвастовством.</p><p>В этот момент Аква показала мне что-то вроде каталога.</p><p>— Пожалуйста, выбери. Я могу даровать тебе одну и только одну силу, которая подойдёт любому. Это может быть уникальная могущественная способность или возможность. Например, легендарное оружие. Давай, это может быть что угодно. У тебя есть специальное право принести с собой одну эту штуку в другой мир.</p><p>Услышав пояснения Аквы, я взял каталог и начал его просматривать.</p><p>...\"Чудовищная сила\", \"суперволшебство\", \"священный меч Арондайт\", \"демонический клинок Мурасаме\"... И множество других всевозможных наименований.</p><p>Так, надо выбрать способность или оружие, чтобы взять с собой.</p><p>Как проблематично, я не мог решить, потому что у меня было слишком много вариантов.</p><p>Или, вернее, мои геймерские инстинкты говорили мне, что все эти способности и снаряжение были нечестными.</p><p>Как трудно, как трудно... Так как я отправлюсь в мир с магией, я бы действительно хотел попробовать ей воспользоваться. Следовательно, я должен выбрать способность, основанную на магии...</p><p>— Аах~ Поторопись~ Всё равно, что ты выберешь. Я ничего не ожидаю от хикикомори и задрота-игромана. Может, ты просто выберешь что-то одно и отправишься по своим делам? Что угодно сгодится, давай скорее~ Скорее уже~</p><p>— Я-я не задрот!.. И я умер не дома, поэтому я не хикикомори!..</p><p>Когда я ответил ей дрожащим голосом, Аква просто перебирала кончики своих волос. Без какого-либо интереса она сказала мне:</p><p>— Это не важно, просто поторопись и выбери~ Кроме тебя есть ещё много ожидающих своей очереди мёртвых душ!</p><p>Пока Аква говорила, она уселась на своё кресло и принялась что-то есть, даже не смотря в мою сторону...</p><p>...Эта девка, которая высмеивает обстоятельства моей кончины, хотя мы встретились впервые. Она ведёт себя настолько самодовольно, потому что она привлекательна.</p><p>Беспечность Аквы разозлила меня.</p><p>Ты хотела, чтобы я выбрал быстро, так?</p><p>Тогда я просто сделаю это.</p><p>\"Что-то\", что я могу принести с собой в тот мир?</p><p>— ...Ладно, я выбираю тебя, — сказал я и указал на Акву.</p><p>Аква с удивлением глянула на меня, продолжая что-то жевать.</p><p>— Ага. Пожалуйста, не выходи из центра магического круга...</p><p>Внезапно Аква замолкла.</p><p>— ...Что ты только что сказал?</p><p>И в следующее мгновение:</p><p>— Поняла. Аква-сама, тогда с этого момента я беру на себя вашу работу.</p><p>С ярко-белой вспышкой света из ниоткуда появилась крылатая женщина.</p><p>...Попросту говоря, это была женщина, выглядящая как ангел.</p><p>— ...Э?</p><p>Под ногами вскрикнувшей от изумления Аквы и под моими ногами появился синий магический круг.</p><p>Ох, что это такое?</p><p>Я действительно отправляюсь в другой мир?</p><p>— Постойте, ха, что происходит? Э, вы шутите? Нет, нет, стоп, это слишком дико! Разве честно брать с собой богиню?! Это же не считается? Такой выбор не должен считаться! Стой! Погоди, ладно?</p><p>Аква запаниковала, у неё на глазах были слёзы, а сама она была в полной растерянности.</p><p>Ангел заговорила, повернувшись лицом к Акве:</p><p>— Бон вояж, Аква-сама. Пожалуйста, остальное оставьте мне. Мы сразу же отправим за вами посланников, чтобы поприветствовать ваше возвращение, когда Король Демонов будет побеждён. До вашего возвращения я возьму на себя все ваши задачи.</p><p>— Стой! Стой! Я богиня, у меня есть только лечащие навыки, а не боевые! Просто невозможно, чтобы я победила Короля Демонов!</p><p>Ангел, чьё появление было настолько внезапным, проигнорировала Акву, рухнувшую в слезах на пол, и мягко улыбнулась мне.</p><p>— Сато Казума-сан. Сейчас вы отправитесь в другой мир и станете одним из героев, претендующих на убийство Короля Демонов. В тот момент, когда вы победите Короля Демонов, вы получите божественный дар.</p><p>— ...Дар? — переспросил я.</p><p>Ангел тепло улыбнулась мне.</p><p>— Верно, дар, достойный спасителя мира... Вам будет даровано исполнение желания, которое может быть абсолютно любым.</p><p>— О!</p><p>Значит, я смогу пожелать вернуться в Японию, если мне надоест тот мир.</p><p>Или, к примеру, устав от того мира, можно вернуться в Японию, стать богатым и проводить весь день за играми в окружении дам! Исполнение даже такой низменной мечты было возможно!</p><p>— Стой! Это мне полагается произносить эту крутую речь!</p><p>После того как ангел взяла на себя работу Аквы, та лишь причитала на полу.</p><p>Я был удовлетворён, увидев Акву в таком состоянии, и поэтому я указал на неё и сказал:</p><p>— Ну и каково это — оказаться напарником человека, на которого ты смотрела свысока? Эй, ты и есть та \"штука\", которую я решил взять с собой. Так как ты богиня, постарайся превратить моё приключение в лёгкую прогулку!</p><p>— Нет~! Чтобы отправиться в другой мир с кем-то вроде него~!</p><p>— Герой! И да будете вы тем, кто из множества других претендующих героев принесёт победу и повергнет Короля Демонов... Ну а теперь я прощаюсь с вами!!</p><p>— Уаааах~! Это была моя реплика~!</p><p>Когда ангел завершила свою речь, яркий свет окутал меня и плачущую Акву!..</p></div>")
                .selectFirst("div.reader-container");
        content.select("br").append("\n");
        content.select("p").append("\n");
        String text="";
        for(int i=0;i<content.childrenSize();i++){
            Element e=content.child(i);
            if(e.hasText()){
                text+=e.wholeText();
            }else{

            }
        }
        content.select("div.article-image").forEach(e->e.replaceWith(new Element(Tag.valueOf("img"),e.baseUri(),new Attributes().put("src",e.select("img").attr("src")))));
        println(content);
    }
}
