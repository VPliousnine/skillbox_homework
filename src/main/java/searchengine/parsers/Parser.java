package searchengine.parsers;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import searchengine.model.Page;
import searchengine.model.PageWithMessage;
import searchengine.services.Storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class Parser {

    public static HashMap<String, Integer> parsePage4Lemmas(String htmlString) {
        try {
            HashMap<String, Integer> result = new HashMap<>();
            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
            for (String word : Jsoup.parse(htmlString).text().split("\\s+")) {
                if (/*(word.length() < 3) ||*/ !word.matches("[А-Яа-я\\-]+")) {
                    continue;
                }
                luceneMorph.getMorphInfo(word.toLowerCase()).forEach(description -> {
                            String[] items = description.replace("|", " ").split("\\s");
                            if (!items[2].equals("ПРЕДЛ") && !items[2].equals("МЕЖД") && !items[2].equals("СОЮЗ") && !items[2].equals("ЧАСТ")) {
                                if (result.containsKey(items[0])) {
                                    result.put(items[0], result.get(items[0]) + 1);
                                } else {
                                    result.put(items[0], 1);
                                }
                            }
                        }
                );
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static PageWithMessage getHTMLPage (String path, String userAgent) {
        PageWithMessage result = new PageWithMessage();
        Page page = new Page();
        try {
            Connection.Response response = Jsoup.connect(path).userAgent(userAgent).timeout(10000).execute();
            page.setCode(response.statusCode());
            page.setContent(response.parse().toString());
            result.setPage(page);
        } catch (Exception ex) {
            System.out.println(ex.getMessage() + " " + path);
            Storage.badAddresses.add(path);
            page.setContent("");
            result.setPage(page);
            result.setMessage(ex.getMessage() + " " + path);
        }
        return result;
    }

    public static String createSnippet(String text, HashMap<String, Integer> lemmas) {
        try {
            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
            String result = "";
            int SNIPPET_LENGTH = 300;
            HashSet<String> words = new HashSet<>();

            for (String word : text.split("\\s+")) {
                if (!word.matches("[А-Яа-я\\-]+") || words.contains(word)) {
                    continue;
                }
                words.add(word);

                for (String description : luceneMorph.getMorphInfo(word.toLowerCase())) {
                    String[] items = description.replace("|", " ").split("\\s");
                    if (lemmas.containsKey(items[0])) {
                        if (result.isEmpty()) {
                            result = text.substring(text.indexOf(word));
                            if (result.length() > SNIPPET_LENGTH) {
                                result = result.substring(0, SNIPPET_LENGTH);
                            }
                        }
                        result = result.replaceAll("(?<!\\\\S)" + word + "(?!\\\\S)", "<b>" + word + "</b>");
                        break;
                    }
                }
            }
            int openTagIndex = result.indexOf("<b><b>");
            while (openTagIndex != -1) {
                int closeTagIndex = result.indexOf("</b>", openTagIndex);
                result = result.substring(0, closeTagIndex) + result.substring(closeTagIndex + 4);
                result = result.substring(0, openTagIndex) + result.substring(openTagIndex + 3);
                openTagIndex = result.indexOf("<b><b>");
            }

            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
