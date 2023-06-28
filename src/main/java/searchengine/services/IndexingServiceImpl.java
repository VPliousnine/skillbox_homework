package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.config.UserAgent;
import searchengine.dto.OKResponse;
import searchengine.dto.Response;
import searchengine.dto.MessageResponse;
import searchengine.model.Page;
import searchengine.model.PageWithMessage;
import searchengine.model.SiteItem;
import searchengine.parsers.Parser;
import searchengine.repo.IndexRepository;
import searchengine.repo.LemmaRepository;
import searchengine.repo.PageRepository;
import searchengine.repo.SiteRepository;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final UserAgent userAgent;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;

    @Override
    public Response startIndexing() {
        if (Storage.getIsIndexing()) {
            return new MessageResponse("Индексация уже запущена", HttpStatus.I_AM_A_TEAPOT);
        }

        Storage.resetCounter();
        Storage.setIsIndexing(true);

        List<Site> sitesList = sites.getSites();
        Thread[] threads = new Thread[sitesList.size()];
        int threadId = 0;
        for (Site site : sitesList) {
            threads[threadId++] = new StartIndexing(site.getUrl(), site.getName(), new DBC(siteRepository, pageRepository, lemmaRepository, indexRepository), userAgent.getAgent());
        }
        for (Thread thread : threads) {
            thread.start();
        }

        return new OKResponse();
    }

    @Override
    public Response stopIndexing() {

        if (!Storage.getIsIndexing()) {
            return new MessageResponse("Индексация не запущена", HttpStatus.I_AM_A_TEAPOT);
        }
        Storage.setIsIndexing(false);

        return new OKResponse();
    }

    @Override
    public Response indexPage(String url) {
        if (url.isEmpty()) {
            return new MessageResponse("Введена пустая строка", HttpStatus.BAD_REQUEST);
        }

        int lastSlash = url.lastIndexOf("/");
        if (lastSlash == -1) {
            return new MessageResponse("Строка не распознана в качестве адреса", HttpStatus.BAD_REQUEST);
        }

        List<Site> sitesList = sites.getSites();
        boolean found = false;
        for (Site site: sitesList) {
            found = url.startsWith(site.getUrl());
            if (found) { break; }
        }
        if (!found) {
            return new MessageResponse("Данная страница находится за пределами сайтов,\n" +
                    "указанных в конфигурационном файле", HttpStatus.BAD_REQUEST);
        }

        DBC dbc = new DBC(siteRepository, pageRepository, lemmaRepository, indexRepository);

        SiteItem siteItem = dbc.getSite(url);
        if (siteItem == null) {
            return new MessageResponse("Сайт не индексирован", HttpStatus.BAD_REQUEST);
        }
        int siteId = siteItem.getId();
        PageWithMessage parsedPage = Parser.getHTMLPage(url, userAgent.getAgent());
        if (parsedPage.getMessage() != null) {
            siteRepository.setLastError(parsedPage.getMessage(), siteId);
        }
        Page page2save = parsedPage.getPage();
        if (page2save.getContent().isEmpty()) {
            return new MessageResponse("Страницу не удалось прочитать", HttpStatus.BAD_REQUEST);
        }
        String url_ = url + (url.equals(siteItem.getUrl()) ? "/" : "");
        Page pageInDB = dbc.getPage(url_, siteItem.getUrl(), siteId);
        if (pageInDB != null) {
            dbc.deletePage(pageInDB.getId());
        }

        String path2Save = url_.substring(siteItem.getUrl().length());
        page2save.setSiteId(siteId);
        page2save.setPath(path2Save.isEmpty() ? "/" : path2Save);

        dbc.savePage(page2save);
        HashMap<String, Integer> lemmas = Parser.parsePage4Lemmas(page2save.getContent());
        dbc.saveLemmas(siteId, dbc.getPage(url_, siteItem.getUrl(), siteId).getId(), lemmas);

        return new OKResponse();
    }

}
