package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.repo.IndexRepository;
import searchengine.repo.LemmaRepository;
import searchengine.repo.PageRepository;
import searchengine.repo.SiteRepository;

import java.util.HashMap;
import java.util.Set;

@Service
public class DBC {

    final private SiteRepository siteRepository;
    final private PageRepository pageRepository;
    final private LemmaRepository lemmaRepository;
    final private IndexRepository indexRepository;

    public DBC(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    public SiteItem getSite(String url) {
        String url_ = url.toLowerCase();
        if (!url_.startsWith("http://") && !url_.startsWith("https://")) { return null; }
        int thirdSlash = url_.indexOf("/", 10);
        if (thirdSlash != -1) {
            url_ = url_.substring(0, thirdSlash);
        }
        return siteRepository.findByUrl(url_);
    }

    public Page getPage(String path, String root, int siteId) {
        String path2Find = path.equals(root) ? "/" : path.substring(root.length());
        return pageRepository.findBySiteIdAndPath(siteId, path2Find);
    }

    public void updateStatusTimeById(int siteId) {
        siteRepository.updateStatusTimeById(siteId);
    }

    SiteItem findSiteById(int id) {
        return siteRepository.findSiteById(id);
    }

    Page findPageById(int id) {
        return pageRepository.findPageById(id);
    }

    public void setStatusToIndexFailed(String lastError, int siteId) {
        siteRepository.setStatusToIndexFailed(lastError, siteId);
    }

    public void setLastError(String lastError, int siteId) {
        siteRepository.setLastError(lastError, siteId);
    }

    public void setStatusToIndexed(int siteId) {
        siteRepository.setStatusToIndexed(siteId);
    }

    public void savePage(Page page) {
        pageRepository.save(page);
    }

    public void saveLemmas(int siteId, int pageId, HashMap<String, Integer> lemmas) {
        lemmas.keySet().forEach(lemma -> {
            lemmaRepository.saveLemma(siteId, lemma);
            int lemmaId = lemmaRepository.getLemma(siteId, lemma).getId();
            indexRepository.saveIndex(pageId, lemmaId, lemmas.get(lemma));
        });
    }

    public void deletePage(int pageId) {
        indexRepository.deleteByPage(pageId);
        lemmaRepository.deleteByPage(pageId);
        pageRepository.deleteById(pageId);
    }

    public HashMap<String, Integer> getLemmas(Set<String> lemmas, Integer siteId, boolean withoutZeroes) {
        HashMap<String, Integer> result = new HashMap<>();
        for (String lemma : lemmas) {
            int frequency = lemmaRepository.getFrequency(siteId, lemma);
            if (frequency == 0 && withoutZeroes) {
                continue;
            }
            result.put(lemma, frequency);
        }
        return result;
    }

    public HashMap<String, Integer> getLemmas(Set<String> lemmas, Integer siteId) {
        return getLemmas(lemmas, siteId, true);
    }

    public HashMap<Integer, Float> getPages(Integer siteId, String lemma) {
        Set<Index> foundPages = indexRepository.getPages(siteId, lemma);
        HashMap<Integer, Float> result = new HashMap<>();
        for (Index item : foundPages) result.put(item.getPageId(), item.getRank());

        return result;
    }

    public SiteItem findSiteByUrl(String url) {
        return siteRepository.findByUrl(url);
    }

    public void deleteDataBySiteId(int siteId) {
        lemmaRepository.deleteBySite(siteId);
        indexRepository.deleteBySite(siteId);
        pageRepository.deleteBySite(siteId);
        siteRepository.deleteById(siteId);
    }

    public void saveSite(SiteItem siteItem) {
        siteRepository.save(siteItem);
    }

    public int getPageCountBySiteId(Integer siteId) {
        return pageRepository.getPageCountBySiteId(siteId);
    }
}
