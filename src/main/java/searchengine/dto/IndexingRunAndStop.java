package searchengine.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicBoolean;
@Data
@Getter
@Setter
public class IndexingRunAndStop {
    private AtomicBoolean indexingRun = new AtomicBoolean();
    private AtomicBoolean indexingStop = new AtomicBoolean();

}
