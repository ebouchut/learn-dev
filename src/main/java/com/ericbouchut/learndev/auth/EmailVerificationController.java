package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.security.Principal;

/**
 * <b>Web</b> endpoints of the email verification flow: the emailed link
 * lands on {@code GET /auth/verify} (public, like the whole {@code /auth}
 * prefix), and a logged-in user can ask for a fresh link from the dashboard
 * banner. The outcome is carried by query flags on the redirect targets,
 * following the house PRG idiom.
 */
@Controller
public class EmailVerificationController {

    private final EmailVerificationService verification;
    private final UserRepository users;

    public EmailVerificationController(
            EmailVerificationService verification,
            UserRepository users
    ) {
        this.verification = verification;
        this.users = users;
    }

    /**
     * Consume the emailed verification link.
     *
     * @param token   the raw token from the link
     * @param request provides the client IP for the audit trail
     * @return a redirect to the login page with a verified or invalid flag
     */
    @GetMapping("/auth/verify")
    public String verify(@RequestParam String token, HttpServletRequest request) {
        boolean verified = verification.verify(token, request.getRemoteAddr());
        return "redirect:/auth/login?" + (verified ? "verified" : "verify-invalid");
    }

    /**
     * Send a fresh verification link to the logged-in user (dashboard
     * banner action).
     *
     * @param principal the logged-in user, or null when the session expired
     * @param request   provides the client IP and the absolute verify URL
     * @return a redirect to the dashboard with a confirmation flag
     */
    @PostMapping("/auth/verify/resend")
    public String resend(Principal principal, HttpServletRequest request) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        User user = users.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + principal.getName()));
        verification.sendVerification(user, request.getRemoteAddr(), verifyUrlBase(request));
        return "redirect:/dashboard?verification-sent";
    }

    static String verifyUrlBase(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/auth/verify")
                .toUriString();
    }
}
