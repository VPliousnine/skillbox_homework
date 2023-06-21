package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.config.UserAgent;
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
public class IndexServiceImpl implements IndexService{

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
    public ResponseEntity<JSONObject> stopIndexing() {
        JSONObject response = new JSONObject();
        if (!Storage.getIsIndexing()) {
            return new ResponseEntity<>(responseError("Индексация не запущена"), HttpStatus.I_AM_A_TEAPOT);
        }
        response.put("result", true);
        Storage.setIsIndexing(false);
        return ResponseEntity.ok(response);
    }

    @Override
    public MessageResponse indexPage(String url) {
        MessageResponse response = new MessageResponse();
        if (url.isEmpty()) {
            response.setMessage("Введена пустая строка");
            return response;
        }

        int lastSlash = url.lastIndexOf("/");
        if (lastSlash == -1) {
            response.setMessage("Строка не распознана в качестве адреса");
            return response;
        }

        List<Site> sitesList = sites.getSites();
        boolean found = false;
        for (Site site: sitesList) {
            found = url.startsWith(site.getUrl());
            if (found) { break; }
        }
        if (!found) {
            response.setMessage("Данная страница находится за пределами сайтов,\n" +
                    "указанных в конфигурационном файле");
            return response;
        }

        DBC dbc = new DBC(siteRepository, pageRepository, lemmaRepository, indexRepository);

        SiteItem siteItem = dbc.getSite(url);
        if (siteItem == null) {
            response.setMessage("Сайт отсутствует в базе данных");
            return response;
        }
        int siteId = siteItem.getId();
        PageWithMessage parsedPage = Parser.getHTMLPage(url, userAgent.getAgent());
        if (parsedPage.getMessage() != null) {
            siteRepository.setLastError(parsedPage.getMessage(), siteId);
        }
        Page page2save = parsedPage.getPage();
        if (page2save.getContent().isEmpty()) {
            response.setMessage("Страницу не удалось прочитать");
            return response;
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

        response.setResult(true);
        return response;
    }

    private static JSONObject responseError(String message) {
        JSONObject response = new JSONObject();
        response.put("result", false);
        response.put("error", message);
        return response;
    }
}
