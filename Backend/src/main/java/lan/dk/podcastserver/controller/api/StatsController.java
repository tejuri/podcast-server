package lan.dk.podcastserver.controller.api;

import io.vavr.collection.List;
import lan.dk.podcastserver.business.stats.StatsBusiness;
import lan.dk.podcastserver.business.stats.StatsPodcastType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by kevin on 28/04/15 for Podcast-Server
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsBusiness itemStatsBusiness;

    @PostMapping("byDownloadDate")
    @Cacheable(value = "stats", key = "{#root.methodName, #numberOfMonth}")
    public List<StatsPodcastType> byDownloadDate(@RequestBody Integer numberOfMonth) {
        return itemStatsBusiness.allStatsByTypeAndDownloadDate(numberOfMonth);
    }

    @PostMapping("byCreationDate")
    @Cacheable(value = "stats", key = "{#root.methodName, #numberOfMonth}")
    public List<StatsPodcastType> byCreationDate(@RequestBody Integer numberOfMonth) {
        return itemStatsBusiness.allStatsByTypeAndCreationDate(numberOfMonth);
    }

    @PostMapping("byPubDate")
    @Cacheable(value = "stats", key = "{#root.methodName, #numberOfMonth}")
    public List<StatsPodcastType> byPubDate(@RequestBody Integer numberOfMonth) {
        return itemStatsBusiness.allStatsByTypeAndPubDate(numberOfMonth);
    }
}
