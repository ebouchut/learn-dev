package com.ericbouchut.learndev.legal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * <b>Web</b> endpoints for the legal pages.
 * Currently the privacy policy (GDPR information duty, art. 13);
 * future legal pages (terms of use, legal notice) belong here too.
 */
@Controller
public class LegalController {

    /**
     * Display the privacy policy page (in French, the language of the
     * supervisory framework this policy addresses).
     * @return the name of the Thymeleaf template for the privacy policy
     */
    @GetMapping("/privacy")
    public String privacy() {
        return "privacy";
    }
}
