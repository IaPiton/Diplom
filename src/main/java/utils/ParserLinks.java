package utils;

import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.config.ParserConfig;
import searchengine.dto.IndexingRunAndStop;
import searchengine.model.Site;
import searchengine.services.DateBaseService;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveAction;

@Setter
public class ParserLinks extends RecursiveAction {
    private final String url;
    private final CopyOnWriteArraySet<String> linksSet;
    private final Site site;
    private int codeResponse;
    private ParserConfig parserConfig;
    private DateBaseService dateBaseService;
    @Autowired
    private IndexingRunAndStop indexingRunAndStop = new IndexingRunAndStop();
    public int getCodeResponse() {
        return codeResponse;
    }

    public ParserLinks(String url, CopyOnWriteArraySet<String> linksSet, Site site) {
        this.url = url;
        this.linksSet = linksSet;
        this.site = site;

    }

    @Override
    protected void compute() {

        if (!indexingRunAndStop.getIndexingStop().get()) {

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
                                    && !(absLink.contains("#")) && absLink.length() > url.length()) {
                                linksChildren.add(absLink);
                            }
                        }
                        for (String childLink : linksChildren) {
                            try {
                                Thread.sleep(150);
                            } catch (InterruptedException e) {
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
                } catch (NullPointerException | ParserConfigurationException | IOException | SQLException ex) {
                    dateBaseService.updateLastError(site, url + " - " + ex.getMessage());
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
            codeResponse = 404;
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
