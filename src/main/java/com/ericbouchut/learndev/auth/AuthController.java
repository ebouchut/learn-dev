package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.auth.dto.RegisterForm;
import com.ericbouchut.learndev.auth.exception.DuplicateEmailException;
import com.ericbouchut.learndev.auth.exception.DuplicateUsernameException;
import com.ericbouchut.learndev.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * <b>Web</b> endpoints for authentication pages:
 * home and login views, and the registration form
 * (display and submission).
 * The dashboard lives in the course package (it renders enrollments).
 * Spring Security handles the login POST and logout itself.
 * This controller renders the pages around them.
 */
@Controller
public class AuthController {

    private final RegistrationService registration;
    private final EmailVerificationService verification;

    public AuthController(RegistrationService registration,
                          EmailVerificationService verification) {
        this.registration = registration;
        this.verification = verification;
    }

    /**
     * GET /
     * Display the home page.
     * @return the name of the Thymeleaf template (aka. View name) for the home page
     * The View will read and render this template.
     */
    @GetMapping("/")
    public String home() {
        return "home";
    }

    /**
     * Display the login form.
     * @return the name of the login template
     */
    @GetMapping("/auth/login")
    public String login() {
        return "login";
    }

    /**
     * Display the User registration form.
     * @param model
     * @return the key of the registration Thymeleaf template
     */
    @GetMapping("/auth/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterForm("", "", ""));
        return "register";
    }

    /**
     * Processes a submitted registration form.
     * Bean-validation failures and duplicate username/email
     * are turned into field errors so the form is re-rendered with the user's input.
     * A successful registration redirects to the login page
     * with an {@code registered} URL query parameter.
     *
     * @param form      the submitted form, validated by {@code @Valid}
     * @param binding   collects validation and duplicate-field errors
     * @return the view name to render, or a redirect on success
     */
    @PostMapping("/auth/register")
    public String register(
            @Valid
            @ModelAttribute("form")
            RegisterForm form,

            BindingResult binding,
            HttpServletRequest request
    ) {
        if (binding.hasErrors()) {
            return "register";
        }
        User user;
        try {
            user = registration.register(form);
        } catch (DuplicateUsernameException e) {
            binding.rejectValue("username", "duplicate", "Username already taken");
            return "register";
        } catch (DuplicateEmailException e) {
            binding.rejectValue("email", "duplicate", "Email already registered");
            return "register";
        }
        verification.sendVerification(user, request.getRemoteAddr(),
                EmailVerificationController.verifyUrlBase(request));
        return "redirect:/auth/login?registered";
    }
}
