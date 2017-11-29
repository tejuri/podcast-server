package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Podcast;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by kevin on 22/02/15.
 */
public interface Finder {

    Podcast find(String url);
    Integer compatibility(@NotEmpty String url);
}
