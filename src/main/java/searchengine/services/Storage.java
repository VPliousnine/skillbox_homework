package searchengine.services;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Storage {
    private static AtomicBoolean isIndexing = new AtomicBoolean(false);
    private static AtomicInteger threadCount = new AtomicInteger(0);
    public static HashSet<String> badAddresses = new HashSet<>();

    public static void setIsIndexing(boolean value) {
        isIndexing.set(value);
    }

    public static boolean getIsIndexing() {
        return isIndexing.get();
    }

//    public static void decrementThreads() { threadCount.decrementAndGet(); }
//    public static void incrementThreads() { threadCount.incrementAndGet(); }
//    public static int getThreadCount() {
//        return threadCount.get();
//    }

}
