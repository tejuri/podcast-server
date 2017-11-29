package lan.dk.podcastserver.scheduled;

import lan.dk.podcastserver.business.update.UpdatePodcastBusiness;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 26/12/2013.
 */
@Component
@RequiredArgsConstructor
public class ItemDeletionScheduled {

    private final UpdatePodcastBusiness updatePodcastBusiness;

    @Scheduled(fixedDelay = 86400000)
    public void deleteOldItem() {
        updatePodcastBusiness.deleteOldEpisode();
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void deleteOldCover() {
        updatePodcastBusiness.deleteOldCover();
    }
}
