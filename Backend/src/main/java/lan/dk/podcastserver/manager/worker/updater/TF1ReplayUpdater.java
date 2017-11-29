package lan.dk.podcastserver.manager.worker.updater;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.SignatureService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static lan.dk.podcastserver.service.HtmlService.toElements;
import static lan.dk.podcastserver.utils.MatcherExtractor.PatternExtractor;
import static lan.dk.podcastserver.utils.MatcherExtractor.from;

/**
 * Created by kevin on 20/07/2016
 */
@Slf4j
@Component("TF1ReplayUpdater")
public class TF1ReplayUpdater extends AbstractUpdater {

    private static final PatternExtractor CHANNEL_PROGRAM_EXTRACTOR = from(Pattern.compile("[^:]+://www.tf1.fr/([^/]+)/([^/]+)/videos.*"));
    private static final String AJAX_URL_FORMAT = "http://www.tf1.fr/ajax/%s/%s/videos?filter=%s";
    private static final String SCHEME_DEFAULT = "https:";
    private static final String DOMAIN = "https://www.tf1.fr";
    private static final String REPLAY_CATEGORY = "replay";
    private static final String ALL_CATEGORY = "all";
    private static final Set<String> TYPES = HashSet.of("replay", "vidéo", "");

    private final HtmlService htmlService;
    private final ImageService imageService;
    private final JsonService jsonService;

    public TF1ReplayUpdater(PodcastServerParameters podcastServerParameters, SignatureService signatureService, Validator validator, HtmlService htmlService, ImageService imageService, JsonService jsonService) {
        super(podcastServerParameters, signatureService, validator);
        this.htmlService = htmlService;
        this.imageService = imageService;
        this.jsonService = jsonService;
    }


    @Override
    public Set<Item> getItems(Podcast podcast) {
        return getHtmlFromStandardOrReplay(podcast.getUrl())
                .map(this::itemsFromHtml)
                .getOrElse(HashSet::empty);
    }

    private Set<Item> itemsFromHtml(Elements els) {
        return HashSet.ofAll(els).map(this::getItem);
    }

    private Item getItem(Element e) {
        String link = e.select(".videoLink").attr("href");
        String url = StringUtils.startsWith(link, "/") ? DOMAIN + link : link;

        return Item.builder()
                    .title(getTitle(e))
                    .description(e.select("p.stitle").text())
                    .pubDate(getDate(url))
                    .url(url)
                    .cover(getCover(e))
                .build();
    }

    private String getTitle(Element v) {
        String text = v.select("p.title").text();
        if (text.contains(" - ")) {
            return StringUtils.substringAfter(text, " - ").trim();
        }

        return text;
    }

    private Cover getCover(Element e) {
        String[] sources = e.select("source").first().attr("data-srcset").split(",");
        String url = List.of(List.of(sources).last().split(" ")).get();
        return imageService.getCoverFromURL(SCHEME_DEFAULT + url);
    }

    private ZonedDateTime getDate(String url) {
        return htmlService.get(url)
                .map(d -> d.select("script[type=application/ld+json]"))
                .map(List::ofAll)
                .getOrElse(List::empty)
                .headOption()
                .map(Element::html)
                .map(jsonService::parse)
                .map(JsonService.to(TF1ReplayItem.class))
                .map(TF1ReplayItem::getUploadDate)
                .getOrElse(ZonedDateTime::now);
    }

    @Override
    public String signatureOf(Podcast podcast) {
        // Url origine : http://www.tf1.fr/tf1/19h-live/videos
        // Url replay : http://www.tf1.fr/ajax/tf1/19h-live/videos?filter=replay
        return getHtmlFromStandardOrReplay(podcast.getUrl())
                .map(Elements::html)
                .map(signatureService::generateMD5Signature)
                .getOrElse(StringUtils.EMPTY);
    }

    private Option<Elements> getHtmlFromStandardOrReplay(String url) {
        Option<Elements> replays = getElementsFrom(url, REPLAY_CATEGORY);

        if(replays.map(ArrayList::size).getOrElse(0) == 0) {
            replays = getElementsFrom(url, ALL_CATEGORY);
        }

        return replays;
    }

    private Option<Elements> getElementsFrom(String url, @NotNull  String inCategory) {
        return CHANNEL_PROGRAM_EXTRACTOR.on(url).groups()
                .map(l -> String.format(AJAX_URL_FORMAT, l.get(0), l.get(1), inCategory))
                .flatMap(jsonService::parseUrl)
                .map(JsonService.to(TF1ReplayResponse.class))
                .map(TF1ReplayResponse::getHtml)
                .map(htmlService::parse)
                .map(d -> d.select(".video"))
                .map(this::getElementsElementsFunction);
    }

    private Elements getElementsElementsFunction(Elements elements) {
        return elements
                .stream()
                .filter(e -> StringUtils.isNotEmpty(e.attr("data-id")))
                .filter(this::isReplayOrVideo)
                .collect(toElements());
    }

    private Boolean isReplayOrVideo(Element element) {
        return TYPES.contains(StringUtils.lowerCase(element.select(".uptitle strong").text()));
    }

    @Override
    public Type type() {
        return new Type("TF1Replay", "TF1 Replay");
    }

    @Override
    public Integer compatibility(String url) {
        return StringUtils.contains(url, "www.tf1.fr") ? 1 : Integer.MAX_VALUE;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TF1ReplayResponse {
        @Setter @Getter String html;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TF1ReplayItem {
        @Setter @Getter ZonedDateTime uploadDate;
    }
}
