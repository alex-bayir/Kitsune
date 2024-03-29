<div align="center">
    <img src="content/logo_rounded.png">
    <br/>
    <img src="https://img.shields.io/badge/install_size-4.0 MB-brightgreen">
    <img src="https://img.shields.io/badge/version-1.8.3-blueviolet">
    <img src="https://img.shields.io/badge/android-7.0+-yellow">
    <br/>
    <img src="https://img.shields.io/badge/manga_sources-11-brightgreen"/> <img src="https://img.shields.io/badge/ranobe_sources-1-brightgreen"/>
    <h1>Kitsune</h1>
    <p>Universal scripted manga and ranobe reader with the ability to save chapters.</p>
</div>

Restarting the [Kitsune(Yotsuba)](https://4pda.to/forum/index.php?showtopic=961133 "4pda") project.<br/><br/>
Features of Kitsune:
- No advertising
- Beautiful design
- Downloading chapters and reading them without the Internet
- Support for custom sources: you can write yourself on lua by looking at the [api](https://github.com/alex-bayir/Kitsune/blob/master/Scripts%20API.md "Scripts API") or sample scripts from the sources listed below
- Integrated translator
- Quick translator call
- Check manga and ranobe for new chapters and more
- Sources of manga/manhwa:

| status |                                                                  ico                                                                  |                    Source                    |      Type      | Language |                                                     Script                                                      |
|:------:|:-------------------------------------------------------------------------------------------------------------------------------------:|:--------------------------------------------:|:--------------:|:--------:|:---------------------------------------------------------------------------------------------------------------:|
|   ✅   |        ![](https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=24&url=http://desu.me)        |           [Desu](https://desu.me)            | Manga / Manhwa |    Ru    |     [Lua](https://github.com/alex-bayir/Kitsune/blob/master/app/src/main/assets/scripts/Desu.lua "Script")      | 
|   ✅   |      ![](https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=24&url=http://remanga.org)      |        [Remanga](https://remanga.org)        | Manga / Manhwa |    Ru    |    [Lua](https://github.com/alex-bayir/Kitsune/blob/master/app/src/main/assets/scripts/Remanga.lua "Script")    | 
|   ✅   |      ![](https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=24&url=http://mangalib.me)      |       [MangaLib](https://mangalib.me)        | Manga / Manhwa |    Ru    |   [Lua](https://github.com/alex-bayir/Kitsune/blob/master/app/src/main/assets/scripts/MangaLib.lua "Script")    | 
|   ✅   |     ![](https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=24&url=http://ranobelib.me)      |      [RanobeLib](https://ranobelib.me)       | Novel / Ranobe |    Ru    |   [Lua](https://github.com/alex-bayir/Kitsune/blob/master/app/src/main/assets/scripts/RanobeLib.lua "Script")   | 
|   ✅   |     ![](https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=24&url=http://hentailib.me)      |    [HentaiLib](https://v1.hentailib.org)     | Manga / Manhwa |    Ru    |   [Lua](https://github.com/alex-bayir/Kitsune/blob/master/app/src/main/assets/scripts/HentaiLib.lua "Script")   | 
|   ✅   |    ![](https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=24&url=http://readmanga.live)     |     [Readmanga](https://readmanga.live)      | Manga / Manhwa |    Ru    |   [Lua](https://github.com/alex-bayir/Kitsune/blob/master/app/src/main/assets/scripts/ReadManga.lua "Script")   | 
|   ✅   |    ![](https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=24&url=http://mintmanga.live)     |     [Mintmanga](https://mintmanga.live)      | Manga / Manhwa |    Ru    |   [Lua](https://github.com/alex-bayir/Kitsune/blob/master/app/src/main/assets/scripts/MintManga.lua "Script")   | 
|   ✅   |    ![](https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=24&url=http://selfmanga.live)     |     [Selfmanga](https://selfmanga.live)      | Manga / Manhwa |    Ru    |   [Lua](https://github.com/alex-bayir/Kitsune/blob/master/app/src/main/assets/scripts/SelfManga.lua "Script")   | 
|   ✅   |     ![](https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=24&url=http://manga-chan.me)     |      [MangaChan](https://manga-chan.me)      | Manga / Manhwa |    Ru    |   [Lua](https://github.com/alex-bayir/Kitsune/blob/master/app/src/main/assets/scripts/MangaChan.lua "Script")   | 
|   ✅   | ![](https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=24&url=http://xxxxx.hentaichan.live) | [HentaiChan](https://xxxxx.hentaichan.live)  | Manga / Manhwa |    Ru    |  [Lua](https://github.com/alex-bayir/Kitsune/blob/master/app/src/main/assets/scripts/HentaiChan.lua "Script")   | 
|   ✅   |  ![](https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=24&url=http://www.mangareader.to)   | [MangaReader.to](https://www.mangareader.to) | Manga / Manhwa | En/Ja/Fr | [Lua](https://github.com/alex-bayir/Kitsune/blob/master/app/src/main/assets/scripts/MangaReaderTo.lua "Script") | 
|   ✅   |   ![](https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=24&url=http://www.mangaread.org)   |    [MangaRead](https://www.mangaread.org)    | Manga / Manhwa |    En    |   [Lua](https://github.com/alex-bayir/Kitsune/blob/master/app/src/main/assets/scripts/Mangaread.lua "Script")   | 

The application is still under development, so there may be errors in the application and changes in the api.

> [!WARNING]
> DISCLAIMER OF LIABILITY
> 
> All copyrights and trademarks belong to their authors. All the content presented in the application is taken from open sources on the Internet.

|      Screenshots       |      Screenshots       |      Screenshots       |
|:----------------------:|:----------------------:|:----------------------:|
|  ![1](/content/1.jpg)  |  ![2](/content/2.jpg)  |  ![3](/content/3.jpg)  |
|  ![4](/content/4.jpg)  |  ![5](/content/5.jpg)  |  ![6](/content/6.jpg)  |
|  ![7](/content/7.jpg)  |  ![8](/content/8.jpg)  |  ![9](/content/9.jpg)  |
| ![10](/content/10.jpg) | ![11](/content/11.jpg) | ![12](/content/12.jpg) |


