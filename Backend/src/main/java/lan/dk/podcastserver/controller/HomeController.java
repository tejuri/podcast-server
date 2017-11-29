package lan.dk.podcastserver.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class HomeController {

    @RequestMapping(value = {"", "items", "podcasts", "podcasts/**", "player", "download", "stats"})
    public String home() {
        return "index";
    }
}
