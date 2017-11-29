package lan.dk.podcastserver.manager.worker.updater;

import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;

import java.util.function.Predicate;


public interface Updater {

    Tuple3<Podcast, Set<Item>, Predicate<Item>> NO_MODIFICATION_TUPLE = Tuple.of(null, null, null);

    Tuple3<Podcast, Set<Item>, Predicate<Item>> update(Podcast podcast);

    Set<Item> getItems(Podcast podcast);

    String signatureOf(Podcast podcast);

    default Predicate<Item> notIn(Podcast podcast) {
        return item -> !podcast.contains(item);
    }

    AbstractUpdater.Type type();

    Integer compatibility(String url);
}
