package searchengine.utils;

import lombok.Data;

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
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveAction;


@Data
@Component
@Slf4j
public class ParserLinks extends RecursiveAction {
    private String url;
    private CopyOnWriteArraySet<String> linksSet;
    private Site site;
    private int codeResponse;
    private ParserConfig parserConfig;
    @Autowired
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
            List<ParserLinks> tasks = new ArrayList<>();
            if (linksSet.add(url)) {
                try {
                    Document document = getDocument(url);
                    dateBaseService.addEntitiesToDateBase(document, url, codeResponse, site, 0);
                    Elements resultLinks = document.select("a[href]");
                    if (!(resultLinks == null || resultLinks.size() == 0)) {
                        List<String> linksChildren = new ArrayList<>();

                        for (Element resultLink : resultLinks) {
                            String absLink = resultLink.attr("abs:href");
                            if ((!linksChildren.contains(absLink)) && absLink.startsWith(url)
                                    && !(absLink.contains("#")) && absLink.length() > url.length() && !isFile(url)) {
                                linksChildren.add(absLink);
                            }
                        }
                        for (String childLink : linksChildren) {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                log.info("Произошло прерывание потока");
                            }
                            ParserLinks task = new ParserLinks(childLink, linksSet, site);
                            task.setParserConfig(parserConfig);
                            task.setDateBaseService(dateBaseService);
                            task.fork();

                            tasks.add(task);
                        }
                    }
                    for (ParserLinks task : tasks) {
                        task.join();
                    }
                } catch (NullPointerException ex) {
                    dateBaseService.updateLastError(site, url + " - " + "Страница пустая");
                } catch (ParserConfigurationException | IOException | SQLException ex) {
                    dateBaseService.updateLastError(site, url + " - " + ex.getMessage());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
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
        } catch (HttpStatusException | SocketTimeoutException e) {
            codeResponse = 503;
            System.out.println(e.getLocalizedMessage());
        } catch (UnsupportedMimeTypeException e) {
            codeResponse = 404;
            System.out.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            codeResponse = 404;
        }
        return doc;
    }

    private static boolean isFile(String link) {
        link.toLowerCase();
        return link.contains(".jpg")
                || link.contains(".jpeg")
                || link.contains(".png")
                || link.contains(".gif")
                || link.contains(".webp")
                || link.contains(".pdf")
                || link.contains(".eps")
                || link.contains(".xlsx")
                || link.contains(".doc")
                || link.contains(".pptx")
                || link.contains(".docx")
                || link.contains(".zip")
                || link.contains("?_ga");
    }
}
