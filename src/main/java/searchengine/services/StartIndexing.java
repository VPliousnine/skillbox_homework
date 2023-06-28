package searchengine.services;

import searchengine.parsers.PageParser;
import searchengine.model.SiteItem;
import searchengine.model.StatusType;

import java.util.concurrent.ForkJoinPool;

public class StartIndexing extends Thread {

    final private String url;
    final private int siteId;
    final DBC dbc;
    final String userAgent;

    public StartIndexing(String url, String name, DBC dbc, String userAgent) {
        this.url = url;
        this.dbc = dbc;
        this.userAgent = userAgent;
        System.out.println("***url=" + url + "***");
        SiteItem siteItem = dbc.findSiteByUrl(url);
        if (siteItem != null) {
            int siteId = siteItem.getId();
            dbc.deleteDataBySiteId(siteId);
        }
        dbc.saveSite(new SiteItem(StatusType.INDEXING, url, name));
        this.siteId = dbc.findSiteByUrl(url).getId();
    }

    @Override
    public void run() {
        try {
            ForkJoinPool pool = new ForkJoinPool();

            Integer res = pool.invoke(new PageParser(url, url, this.siteId, dbc, userAgent, true, "/"));

        } catch (Exception ex) {
            System.out.println(ex.fillInStackTrace());
        }
    }

}
