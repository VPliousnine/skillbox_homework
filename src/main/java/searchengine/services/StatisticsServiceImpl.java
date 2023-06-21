package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.config.UserAgent;
import searchengine.dto.SearchItem;
import searchengine.dto.SearchResponse;
import searchengine.dto.statistics.*;
import searchengine.parsers.Parser;
import searchengine.model.*;
import searchengine.repo.IndexRepository;
import searchengine.repo.LemmaRepository;
import searchengine.repo.PageRepository;
import searchengine.repo.SiteRepository;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

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
    public void startIndexing() {
        List<Site> sitesList = sites.getSites();
        Thread[] threads = new Thread[sitesList.size()];
        int threadId = 0;
        for (Site site : sitesList) {
            threads[threadId++] = new StartIndexing(site.getUrl(), site.getName(), new DBC(siteRepository, pageRepository, lemmaRepository, indexRepository), userAgent.getAgent());
        }
        for (Thread thread : threads) {
            thread.start();
        }
    }

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            SiteItem siteItem = siteRepository.findByUrl(site.getUrl());
            int siteId = (siteItem == null ? 0 : siteItem.getId());
            int pages = (siteItem == null ? 0 : pageRepository.getPageCountBySiteId(siteId));
            int lemmas = (siteItem == null ? 0 : lemmaRepository.getCountBySiteId(siteId));
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(siteItem == null ? "NOT INDEXED" : siteItem.getStatus().toString());
            item.setError(siteItem == null ? "Индексирование не запускалось" : siteItem.getLastError());
            item.setStatusTime(siteItem == null ? System.currentTimeMillis() : ZonedDateTime.of(siteItem.getStatusTime(), ZoneId.systemDefault()).toInstant().toEpochMilli());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    public static <K, V extends Comparable<V> > Map<K, V>
    valueSort(final Map<K, V> map, int order)
    {
        Comparator<K> valueComparator = (k1, k2) -> {
            int comp = map.get(k1).compareTo(
                    map.get(k2));
//            if (comp == 0)
//                return 1 * order ;
//            else
                return comp * order;
        };

        Map<K, V> sorted = new TreeMap<>(valueComparator);

        sorted.putAll(map);

        return sorted;
    }

    @Override
    public SearchResponse search(String query, Integer offset, Integer limit, String site) {
        SearchResponse response = new SearchResponse();
        response.setResult(false);
        if (query == null || query.isEmpty()) {
            response.setMessage("Задан пустой запрос");
            return response;
        }
        HashMap<String, Integer> lemmas = Parser.parsePage4Lemmas(query);
        if (lemmas.size() == 0) {
            response.setMessage("Запрос не содержит лемм для поиска");
            return response;
        }
        DBC dbc = new DBC(siteRepository, pageRepository, lemmaRepository, indexRepository);
        Integer siteId;

        if (site != null) {
            siteId = dbc.getSite(site).getId();
            if (siteId == null) {
                response.setMessage("Указанный сайт не проиндексирован");
                return response;
            }
        } else { siteId = null; }

        int allPageCount = dbc.getPageCountBySiteId(siteId);
        Map<String, Integer> sortedLemmas = valueSort(dbc.getLemmas(lemmas.keySet(), siteId, false), 1);

        ConcurrentHashMap<Integer, Float> allFoundPages = new ConcurrentHashMap<>();

        boolean first = true;
        for (String key : sortedLemmas.keySet()) {
            HashMap<Integer, Float> foundPages = dbc.getPages(siteId, key);
            if (first) {
                foundPages.keySet().forEach(k -> allFoundPages.put(k, foundPages.get(k)));
                first = false;
            } else {
                for (Integer k : allFoundPages.keySet()) {
                    if (foundPages.containsKey(k)) {
                        allFoundPages.replace(k, allFoundPages.get(k) + foundPages.get(k));
                    } else {
                        allFoundPages.remove(k);
                    }
                }
            }
            if (allFoundPages.size() == 0) { break; }
        }
        float maxRank;
        Map<Integer, Float> sortedPages;
        if (!allFoundPages.isEmpty()) {
            maxRank = Collections.max(allFoundPages.values());
            sortedPages = valueSort(allFoundPages, -1);
        } else {
            sortedPages = new HashMap<>();
            maxRank = 1;
        }

        int offSetIndex = offset == null ? 0 : offset;
        int limitCount = limit == null ? 20 : limit;
        List<SearchItem> data = new ArrayList<>();

        for (Map.Entry<Integer, Float> entry : sortedPages.entrySet()) {
            if (offSetIndex-- > 0) { continue; }
            Page page = dbc.findPageById(entry.getKey());
            SiteItem siteItem = dbc.findSiteById(page.getSiteId());

            SearchItem dataItem = new SearchItem();
            dataItem.setSite(siteItem.getUrl());
            dataItem.setSiteName(siteItem.getName());
            dataItem.setUri(page.getPath());
            dataItem.setTitle(Jsoup.parse(page.getContent()).title());
            dataItem.setSnippet(Parser.createSnippet(Jsoup.parse(page.getContent()).text(), lemmas));
            dataItem.setRelevance(entry.getValue() / maxRank);
            data.add(dataItem);

            System.out.println(dbc.findSiteById(page.getSiteId()).getUrl() + page.getPath() + " - " + entry.getValue() / maxRank);
            if (--limitCount == 0) { break; }
        }

        response.setResult(true);
        response.setCount(sortedPages.size());
        response.setData(data);

        return response;
    }
}
