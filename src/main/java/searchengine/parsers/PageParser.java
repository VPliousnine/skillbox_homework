package searchengine.parsers;

import org.jsoup.Jsoup;
import searchengine.model.*;
import searchengine.services.DBC;
import searchengine.services.Storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.RecursiveTask;

public class PageParser extends RecursiveTask<Integer> {
    final private String pageAddress;
    final private String root;
    final private int siteId;
    final private int rootLength;
    final DBC dbc;
    static private String userAgent;

    private int statusCode;
    private String pageHTML;
    final private boolean isRoot;
    private String fromPage;

    public PageParser(String pageAddress, String root, int siteId, DBC dbc, String userAgent, boolean isRoot, String fromPage) {
        this.pageAddress = pageAddress;
        this.root = root;
        this.siteId = siteId;
        this.rootLength = root.length();
        this.dbc = dbc;
        this.userAgent = userAgent;
        this.isRoot = isRoot;
        this.fromPage = fromPage;
    }

    private boolean isBadLink (String path) {
        if (path == null) {
            return true;
        }
        if (path.indexOf("?") > 0 || path.indexOf("#") > 0 || path.indexOf("&") > 0) {
            return true;
        }
        if (Storage.badAddresses.contains(path)) {
            return true;
        }
        if (path.endsWith(".pdf") || path.endsWith(".jpg") || path.endsWith(".png") || path.endsWith(".doc") || path.endsWith(".docx")) {
            return true;
        }
        if (!isRoot && path.equals(root)) {
            return true;
        }

        if (dbc.getPage(path, root, siteId) != null) {
            Storage.badAddresses.add(path);
            return true;
        }
        return false;
    }

    private void stopIndexing() {
        Storage.decrementThreads();
        dbc.setStatusToIndexFailed("Индексирование остановлено пользователем", siteId);
    }

    void savePage() {
        if (dbc.getPage(pageAddress, root, siteId) == null) {
            Page page = new Page();
            page.setSiteId(siteId);
            page.setCode(statusCode);
            String path2Save = pageAddress.substring(rootLength);
            page.setPath(path2Save.isEmpty() ? "/" : path2Save);
            page.setContent(pageHTML == null ? "" : pageHTML);
            dbc.savePage(page);
        }
    }

    void parsePage() {
        HashMap<String, Integer> lemmas = Parser.parsePage4Lemmas(pageHTML);
        if (lemmas.size() == 0) {
            System.out.println("Ошибка при распознавании страницы " + pageAddress);
            dbc.setLastError("Ошибка при распознавании страницы " + pageAddress, siteId);
            Storage.badAddresses.add(pageAddress);
            return;
        }
        Page page = dbc.getPage(pageAddress, root, siteId);
        if (page == null) { return; }
        dbc.saveLemmas(siteId, page.getId(), lemmas);
    }

    @Override
    protected Integer compute() {
        int res = 0;

        if (pageAddress == null) {
            return 1;
        }
        if (isBadLink(pageAddress)) {
            return 1;
        }
        if (!Storage.getIsIndexing()) {
            stopIndexing();
            return 1;
        }
        Storage.incrementThreads();

        try {
            Thread.sleep(500L + (long) (Math.random() * 1000));
        } catch (InterruptedException e) {
            Storage.decrementThreads();
            throw new RuntimeException(e);
        }

        PageWithMessage parsedPage = Parser.getHTMLPage(pageAddress, userAgent);
        if (parsedPage.getMessage() != null) {
            dbc.setLastError(parsedPage.getMessage(), siteId);
        }

        pageHTML = parsedPage.getPage().getContent();
        statusCode = parsedPage.getPage().getCode();
        savePage();
        if (statusCode == 200) {
            parsePage();
        }
        if (isRoot && (statusCode != 200 || pageHTML.isEmpty())) {
            dbc.setStatusToIndexFailed("Невозможно получить главную страницу сайта", siteId);
        } else {
            dbc.updateStatusTimeById(siteId);
        }

        if (pageHTML.isEmpty()) {
            Storage.decrementThreads();
            return 1;
        }

        if (!Storage.getIsIndexing()) {
            stopIndexing();
            return 1;
        }

        List<PageParser> taskList = new ArrayList<>();

        HashSet<String> links = new HashSet<>();
        Jsoup.parse(pageHTML).select("a").forEach(el ->
                {
                    String link = el.attr("href");
                    if (!link.equals("/") && link.startsWith("/")) {
                        link = root + link;
                    }
                    if (!Storage.checkedAddresses.contains(link) && !links.contains(link) && Storage.getIsIndexing() && link.startsWith(root) && !isBadLink(link) && !link.equals(pageAddress)) {
                        links.add(link);
                        Storage.checkedAddresses.add(link);
                        PageParser task = new PageParser(link, root, siteId, dbc, userAgent, false, pageAddress);
                        task.fork();
                        taskList.add(task);
                    }
                }
        );

        for (PageParser task : taskList) {
            res = res + task.join();
        }
        Storage.decrementThreads();

        if (isRoot && Storage.getIsIndexing()) {
            dbc.setStatusToIndexed(siteId);
        }
//System.out.println("page = " + pageAddress + " from = " + fromPage + ": (finished) " + Storage.getThreadCount() + " (" + Storage.checkedAddresses.size() + "/" + Storage.badAddresses.size() + ")");

        return res;
    }

}
