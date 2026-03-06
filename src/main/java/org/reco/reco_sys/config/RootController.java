package org.reco.reco_sys.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
public class RootController {

    @GetMapping("/")
    public RedirectView root() {
        return new RedirectView("/swagger-ui/index.html");
    }
}
