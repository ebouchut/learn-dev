package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.auth.dto.ForgotPasswordForm;
import com.ericbouchut.learndev.auth.dto.ResetPasswordForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * <b>Web</b> endpoints for the password-reset flow:
 * the "forgot password" request page and the "reset password" page that
 * consumes the emailed token. Both are anonymous pages under {@code /auth/}.
 */
@Controller
public class PasswordResetController {

    private final PasswordResetService passwordReset;

    public PasswordResetController(PasswordResetService passwordReset) {
        this.passwordReset = passwordReset;
    }

    /**
     * Display the "forgot password" request form.
     * @param model carries the empty form-backing bean
     * @return the name of the forgot-password template
     */
    @GetMapping("/auth/forgot-password")
    public String forgotPasswordForm(Model model) {
        model.addAttribute("form", new ForgotPasswordForm(""));
        return "forgot-password";
    }

    /**
     * Processes a "forgot password" request. The response never reveals
     * whether the email exists (enumeration-safe): every valid submission
     * redirects to the same neutral confirmation.
     *
     * @return redirect to the confirmation state, or the re-rendered form
     *         on validation errors
     */
    @PostMapping("/auth/forgot-password")
    public String requestReset(
            @Valid @ModelAttribute("form") ForgotPasswordForm form,
            BindingResult binding,
            HttpServletRequest request) {
        if (binding.hasErrors()) {
            return "forgot-password";
        }
        String resetUrlBase = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/auth/reset-password")
                .toUriString();
        passwordReset.requestReset(form.email(), request.getRemoteAddr(), resetUrlBase);
        return "redirect:/auth/forgot-password?sent";
    }

    /**
     * Display the "reset password" form when the emailed token is valid;
     * otherwise render the invalid-link state (expired, used, or unknown).
     *
     * @param token the raw token from the emailed link
     * @return the name of the reset-password template
     */
    @GetMapping("/auth/reset-password")
    public String resetPasswordForm(@RequestParam(name = "token", required = false) String token,
                                    Model model) {
        boolean usable = token != null && passwordReset.findUsableToken(token).isPresent();
        model.addAttribute("tokenValid", usable);
        model.addAttribute("form", new ResetPasswordForm(token == null ? "" : token, ""));
        return "reset-password";
    }

    /**
     * Consumes the token and sets the new password. Success redirects to the
     * login page with a confirmation; an unusable token renders the
     * invalid-link state.
     */
    @PostMapping("/auth/reset-password")
    public String resetPassword(
            @Valid @ModelAttribute("form") ResetPasswordForm form,
            BindingResult binding,
            HttpServletRequest request,
            Model model) {
        if (binding.hasErrors()) {
            boolean usable = passwordReset.findUsableToken(form.token()).isPresent();
            model.addAttribute("tokenValid", usable);
            return "reset-password";
        }
        boolean done = passwordReset.resetPassword(
                form.token(), form.password(), request.getRemoteAddr());
        if (!done) {
            model.addAttribute("tokenValid", false);
            return "reset-password";
        }
        return "redirect:/auth/login?reset";
    }
}
