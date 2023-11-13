package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import searchengine.model.Site;

import java.util.ArrayList;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SiteConfig {
    private ArrayList<Site> sites;
}
