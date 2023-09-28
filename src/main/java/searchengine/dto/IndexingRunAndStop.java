package searchengine.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import searchengine.services.IndexingService;

import java.util.concurrent.atomic.AtomicBoolean;
@Data
public class IndexingRunAndStop {
    private volatile AtomicBoolean indexingRun = new AtomicBoolean();
    private volatile AtomicBoolean indexingStop = new AtomicBoolean();



}
