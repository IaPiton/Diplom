package searchengine;
import lombok.Setter;
import searchengine.dto.IndexingRunAndStop;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.RecursiveAction;
public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        IndexingRunAndStop indexingRunAndStop = new IndexingRunAndStop();
//        indexingRunAndStop.getIndexingStop().set(true);
      System.out.println(indexingRunAndStop.getIndexingStop().get());
    }
}



