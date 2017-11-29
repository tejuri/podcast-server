package lan.dk.podcastserver.repository;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.QTag;
import lan.dk.podcastserver.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static io.vavr.API.Option;

/**
 * Created by kevin on 07/06/2014.
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, UUID>, QueryDslPredicateExecutor<Tag> {

    default Option<Tag> findByNameIgnoreCase(String name) {
        return Option(findOne(QTag.tag.name.equalsIgnoreCase(name)));
    }

    default Set<Tag> findByNameContainsIgnoreCase(String name) {
        return HashSet.ofAll(findAll(QTag.tag.name.containsIgnoreCase(name)));
    }

}
