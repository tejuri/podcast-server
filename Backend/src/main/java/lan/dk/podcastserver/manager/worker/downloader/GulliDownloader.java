package lan.dk.podcastserver.manager.worker.downloader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jayway.jsonpath.TypeRef;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.UrlService;
import lan.dk.podcastserver.service.factory.WGetFactory;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.regex.Pattern;

import static io.vavr.API.Lazy;
import static io.vavr.API.None;
import static lan.dk.podcastserver.utils.MatcherExtractor.PatternExtractor;
import static lan.dk.podcastserver.utils.MatcherExtractor.from;

/**
 * Created by kevin on 12/10/2016 for Podcast Server
 */
@Slf4j
@Scope("prototype")
@Component("GulliDownloader")
public class GulliDownloader extends HTTPDownloader {

    private static final PatternExtractor NUMBER_IN_PLAYLIST_EXTRACTOR = from(Pattern.compile("playlistItem\\(([^)]*)\\);"));
    private static final PatternExtractor PLAYLIST_EXTRACTOR = from(Pattern.compile("playlist:\\s*(.*?(?=events:))", Pattern.DOTALL));
    private static final TypeRef<List<GulliItem>> GULLI_ITEM_TYPE_REF = new TypeRef<List<GulliItem>>() { };

    private final HtmlService htmlService;
    private final JsonService jsonService;

    private Lazy<Option<String>> _url = Lazy(this::_getItemUrl);

    public GulliDownloader(ItemRepository itemRepository, PodcastRepository podcastRepository, PodcastServerParameters podcastServerParameters, SimpMessagingTemplate template, MimeTypeService mimeTypeService, UrlService urlService, WGetFactory wGetFactory, HtmlService htmlService, JsonService jsonService) {
        super(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService, urlService, wGetFactory);
        this.htmlService = htmlService;
        this.jsonService = jsonService;
    }

    @Override
    public String getItemUrl(Item item) {
        if (!Objects.equals(this.item, item)) {
            return item.getUrl();
        }

        return _url.get().getOrElseThrow(() -> new RuntimeException("url not found for " + item.getUrl()));
    }

    private Option<String> _getItemUrl() {
        return htmlService.get(item.getUrl())
                .map(d -> d.select("script"))
                .flatMap(scripts -> List.ofAll(scripts).find(e -> e.html().contains("playlist:")))
                .flatMap(this::getPlaylistFromGulliScript);
    }

    private Option<String> getPlaylistFromGulliScript(Element element) {
        Option<String> playlist = PLAYLIST_EXTRACTOR.on(element.html()).group(1);
        return NUMBER_IN_PLAYLIST_EXTRACTOR
                .on(element.html()).group(1)
                .map(Integer::valueOf)
                .flatMap(v -> playlist.map(s -> Tuple.of(v, s)))
                .map(t -> t.map2(jsonService::parse))
                .map(t -> t.map2(JsonService.to(GULLI_ITEM_TYPE_REF)))
                .map(t -> t.apply((v, s) -> s.get(v)))
                .flatMap(i -> i.getSources().find(s -> s.file.contains("mp4")))
                .map(GulliItem.GulliSource::getFile);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GulliItem {
        @Getter @Setter private List<GulliSource> sources;

        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class GulliSource {
            @Getter @Setter private String file;
        }

    }

    @Override
    public Integer compatibility(String url) {
        return url.contains("replay.gulli.fr") ? 1 : Integer.MAX_VALUE;
    }
}
