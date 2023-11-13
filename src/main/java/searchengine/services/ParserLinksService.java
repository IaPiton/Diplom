package searchengine.services;

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
public class ParserLinksService extends RecursiveAction {
    private String url;
    private CopyOnWriteArraySet<String> linksSet;
    private Site site;
    private int codeResponse;
    private ParserConfig parserConfig;
    @Autowired
    private DateBaseService dateBaseService;


    public ParserLinksService() {
    }

    public ParserLinksService(String url, CopyOnWriteArraySet<String> linksSet, Site site) {
        this.url = url;
        this.linksSet = linksSet;
        this.site = site;
    }

    @Override
    protected void compute() {
        if (!IndexingService.getIndexingStop().get()) {
            List<ParserLinksService> tasks = new ArrayList<>();
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
                                    && !(absLink.contains("#")) && absLink.length() > url.length()) {
                                linksChildren.add(absLink);
                            }
                        }
                        for (String childLink : linksChildren) {
                            try {
                                Thread.sleep(350);
                            } catch (InterruptedException e) {
                                log.info("Произошло прерывание потока");
                            }
                            ParserLinksService task = new ParserLinksService(childLink, linksSet, site);
                            task.setParserConfig(parserConfig);
                            task.setDateBaseService(dateBaseService);
                            task.fork();

                            tasks.add(task);
                        }
                    }
                    for (ParserLinksService task : tasks) {
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
                    .timeout(parserConfig.getTimeout());
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
}
