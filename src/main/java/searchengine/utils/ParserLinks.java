package searchengine.utils;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.config.ParserConfig;
import searchengine.model.Site;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveAction;


@Component
@Slf4j
public final class ParserLinks extends RecursiveAction {
    private String url;
    private CopyOnWriteArraySet<String> linksSet;
    private Site site;
    private int codeResponse;
    @Autowired
    @Setter
    private ParserConfig parserConfig;
    @Autowired
    @Setter
    private DateBaseService dateBaseService;

    public ParserLinks() {
    }

    public ParserLinks(String url, CopyOnWriteArraySet<String> linksSet, Site site) {
        this.url = url;
        this.linksSet = linksSet;
        this.site = site;
    }

    @Override
    protected void compute() {
            if (!DateBaseService.getIndexingStop().get()) {
                Set<ParserLinks> tasks = new TreeSet<>(Comparator.comparing(o -> o.url));
                try {
                    Thread.sleep(600);
                    Document document = getDocument(url);
                    dateBaseService.addEntitiesToDateBase(document, url, codeResponse, site);
                    Elements resultLinks = document.select("a[href]");
                    for (Element resultLink : resultLinks) {
                        String absLink = resultLink.attr("abs:href");
                        if (absLink.length() > 255) {
                            continue;
                        }
                        if (!absLink.startsWith(site.getUrl())) {
                            continue;
                        }
                        if (absLink.contains("#")) {
                            continue;
                        }
                        if (isFile(absLink)) {
                            continue;
                        }
                        if (!(linksSet.add(absLink))) {
                            continue;
                        }
                        ParserLinks task = new ParserLinks(absLink, linksSet, site);
                        task.setParserConfig(parserConfig);
                        task.setDateBaseService(dateBaseService);
                        task.fork();
                        tasks.add(task);
                    }
                    for (ParserLinks task : tasks) {
                        task.join();
                    }
                } catch (SocketException | ParserConfigurationException ex) {
                    log.info("SocketException");
                } catch (InterruptedException e) {
                    log.info("ParserConfigurationException");
                } catch (SQLException e) {
                    log.info("SQLException");
                } catch (IOException e) {
                    log.info("IOException");
                }catch (NullPointerException e){
                    log.info("Страница " + url + " пустая");
                }
            }
        }

    public void parserPage (String url, Site site) {
        try {
          Document document = getDocument(url);
        dateBaseService.addEntitiesToDateBase(document, url, codeResponse , site);
        } catch (ParserConfigurationException | SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Document getDocument(String url) throws ParserConfigurationException, SQLException, IOException {
        Document doc = null;
        try {
            Connection connection = Jsoup
                    .connect(url)
                    .userAgent(parserConfig.getUseragent())
                    .referrer(parserConfig.getReferrer())
                    .timeout(parserConfig.getTimeout())
                    .ignoreContentType(true);
            doc = connection.get();
            codeResponse = connection.response().statusCode();
        } catch (SocketTimeoutException e){
            log.info("Превышен тайм-аут соединения");
        } catch (HttpStatusException e) {
            codeResponse = 503;
            log.info("Страница " + url + " не доступна" );
        } catch (UnsupportedMimeTypeException | NullPointerException e) {
            codeResponse = 404;
            log.info("Страница " + url + " не доступна" );
        } catch (IOException e) {
            log.info("Страница " + url + " не доступна" );
            codeResponse = 404;
        }
        return doc;
    }

    private static boolean isFile(String link) {
        return link.toLowerCase().contains(".jpg")
                || link.toLowerCase().contains(".jpeg")
                || link.toLowerCase().contains(".png")
                || link.toLowerCase().contains(".gif")
                || link.toLowerCase().contains(".webp")
                || link.toLowerCase().contains(".pdf")
                || link.toLowerCase().contains(".eps")
                || link.toLowerCase().contains(".xlsx")
                || link.toLowerCase().contains(".doc")
                || link.toLowerCase().contains(".pptx")
                || link.toLowerCase().contains(".docx")
                || link.toLowerCase().contains(".zip")
                || link.toLowerCase().contains("?_ga");
    }

}