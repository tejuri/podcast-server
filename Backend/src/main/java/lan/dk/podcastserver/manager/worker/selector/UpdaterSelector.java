package lan.dk.podcastserver.manager.worker.selector;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import lan.dk.podcastserver.manager.worker.updater.AbstractUpdater;
import lan.dk.podcastserver.manager.worker.updater.NoOpUpdater;
import lan.dk.podcastserver.manager.worker.updater.Updater;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;

import static io.vavr.API.Option;

/**
 * Created by kevin on 06/03/15.
 */
@Service
public class UpdaterSelector {

    static final NoOpUpdater NO_OP_UPDATER = new NoOpUpdater();

    private final Set<Updater> updaters;

    public UpdaterSelector(java.util.Set<Updater> updaters) {
        this.updaters = HashSet.ofAll(updaters);
    }

    public Updater of(String url) {
        return Option(url)
                .filter(StringUtils::isNotEmpty)
                .map(u -> updaters)
                .getOrElse(HashSet.empty())
                .minBy(Comparator.comparing(updater -> updater.compatibility(url)))
                .getOrElse(NO_OP_UPDATER);
    }

    public Set<AbstractUpdater.Type> types() {
        return updaters.map(Updater::type);
    }


}
