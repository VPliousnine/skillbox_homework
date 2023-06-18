package searchengine.methodes;

import org.jsoup.Jsoup;
import searchengine.model.*;
import searchengine.services.DBC;
import searchengine.services.Storage;

import java.time.LocalDateTime;
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

    public PageParser(String pageAddress, String root, int siteId, DBC dbc, String userAgent, boolean isRoot) {
        this.pageAddress = pageAddress;
        this.root = root;
        this.siteId = siteId;
        this.rootLength = root.length();
        this.dbc = dbc;
        this.userAgent = userAgent;
        this.isRoot = isRoot;
    }

    private boolean isBadLink (String path) {
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
//        Storage.decrementThreads();
//        System.out.println(pageAddress + "!".repeat(50));
        dbc.setStatusToIndexFailed("Индексирование остановлено пользователем", siteId);
    }

    public void parsePage() {
        if (dbc.getPage(pageAddress, root, siteId) == null) {
            Page page = new Page();
            page.setSiteId(siteId);
            page.setCode(statusCode);
            String path2Save = pageAddress.substring(rootLength);
            page.setPath(path2Save.isEmpty() ? "/" : path2Save);
            page.setContent(pageHTML == null ? "" : pageHTML);
            dbc.savePage(page);
            HashMap<String, Integer> lemmas = Parser.parsePage4Lemmas(pageHTML);
            dbc.saveLemmas(siteId, dbc.getPage(pageAddress, root, siteId).getId(), lemmas);
        }
    }

    @Override
    protected Integer compute() {
        if (isBadLink(pageAddress)) { return 1; }
        if (!Storage.getIsIndexing()) {
            stopIndexing();
            return 1;
        }
//        Storage.incrementThreads();
//        System.out.println(">".repeat(15) + " " + Thread.currentThread().getName() + " - "+ pageAddress + ", threads: " + Storage.getThreadCount());

        try {
            Thread.sleep(500L + (long) (Math.random() * 1000));
        } catch (InterruptedException e) {
//            Storage.decrementThreads();
            throw new RuntimeException(e);
        }

        PageWithMessage parsedPage = Parser.getHTMLPage(pageAddress, userAgent);
        if (parsedPage.getMessage() != null) {
            dbc.setLastError(parsedPage.getMessage(), siteId);
        }

        pageHTML = parsedPage.getPage().getContent();
        statusCode = parsedPage.getPage().getCode();
        parsePage();
        if (isRoot && (statusCode != 200 || pageHTML.isEmpty())) {
            dbc.setStatusToIndexFailed("Невозможно получить главную страницу сайта", siteId);
        } else {
            dbc.updateStatusTimeById(siteId);
        }

        if (pageHTML.isEmpty()) {
//            Storage.decrementThreads();
            return 1;
        }

        if (!Storage.getIsIndexing()) {
            stopIndexing();
            return 1;
        }

        List<PageParser> taskList = new ArrayList<>();
        List<String> linxList = new ArrayList<>();

        HashSet<String> links = new HashSet<>();
        Jsoup.parse(pageHTML).select("a").forEach(el ->
                {
                    String link = el.attr("href");
                    if (!link.equals("/") && link.startsWith("/")) {
                        link = root + link;
                    }
                    if (!links.contains(link) && Storage.getIsIndexing() && link.startsWith(root) && !isBadLink(link)) {
                        links.add(link);
                        PageParser task = new PageParser(link, root, siteId, dbc, userAgent, false);
                        task.fork();
                        taskList.add(task);
                        linxList.add(link);
                    }
                }
        );
//        System.out.println(">".repeat(15) + " " + Thread.currentThread().getName() + " - "+ pageAddress + " new addresses were added " + taskList.size());
        int res = 0;
        for (int i = 0; i < taskList.size(); i++ ) {
            PageParser task = taskList.get(i);
            res = res + task.join();
//            System.out.println("<<<" + LocalDateTime.now() + " - " + pageAddress + " (" + linxList.get(i) + ") " + task + "<<<" + res);
        }
//        Storage.decrementThreads();
//        System.out.println("threads " + Storage.getThreadCount() + " " + pageAddress + " "
//                            + Thread.currentThread() + ", active is " + Thread.activeCount()
//                );
//        if (isRoot && Storage.getIsIndexing()) {
//            System.out.println(">".repeat(10)+"!".repeat(50));
//            dbc.setStatusToIndexed(siteId);
//            System.out.println("<".repeat(10)+"!".repeat(50));
//        }

        return res;
    }

}
