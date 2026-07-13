package com.ericbouchut.learndev.admin;

import com.ericbouchut.learndev.auth.dto.RegisterForm;
import com.ericbouchut.learndev.auth.exception.DuplicateEmailException;
import com.ericbouchut.learndev.auth.exception.DuplicateUsernameException;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.UUID;

/**
 * <b>Web</b> endpoints of the administration area under {@code /admin/**}
 * (gated to {@code ROLE_ADMIN} by the security rules): the account list,
 * instructor account creation, and account archiving. Rules and the audit
 * trail live in {@link AccountAdminService}.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AccountAdminService accounts;
    private final UserRepository users;

    public AdminController(AccountAdminService accounts, UserRepository users) {
        this.accounts = accounts;
        this.users = users;
    }

    /**
     * Display every account with its roles and active flag.
     *
     * @param model receives the accounts
     * @return the account list view name
     */
    @GetMapping("/users")
    public String userList(Model model) {
        model.addAttribute("accounts", accounts.allUsers());
        return "admin/users";
    }

    /**
     * Display the instructor account creation form.
     *
     * @param model receives an empty form
     * @return the account form view name
     */
    @GetMapping("/users/new-instructor")
    public String newInstructorForm(Model model) {
        model.addAttribute("form", new RegisterForm("", "", ""));
        return "admin/user-form";
    }

    /**
     * Create an instructor account. Duplicate username/email surface as
     * field errors, like self-registration.
     *
     * @param form      the submitted form, validated by {@code @Valid}
     * @param binding   collects validation and duplicate-field errors
     * @param principal the logged-in admin
     * @param request   provides the client IP for the audit trail
     * @return a redirect to the account list, or the re-rendered form
     */
    @PostMapping("/users/new-instructor")
    public String createInstructor(
            @Valid @ModelAttribute("form") RegisterForm form,
            BindingResult binding,
            Principal principal,
            HttpServletRequest request
    ) {
        if (binding.hasErrors()) {
            return "admin/user-form";
        }
        try {
            accounts.createInstructor(form, currentUser(principal), request.getRemoteAddr());
        } catch (DuplicateUsernameException e) {
            binding.rejectValue("username", "duplicate", "Username already taken");
            return "admin/user-form";
        } catch (DuplicateEmailException e) {
            binding.rejectValue("email", "duplicate", "Email already registered");
            return "admin/user-form";
        }
        return "redirect:/admin/users?created";
    }

    /**
     * Archive an account (PRG with an {@code archived} flag).
     *
     * @param userId    the account to archive
     * @param principal the logged-in admin (self-archive guard)
     * @param request   provides the client IP for the audit trail
     * @return a redirect to the account list
     */
    @PostMapping("/users/{userId}/archive")
    public String archiveAccount(
            @PathVariable UUID userId,
            Principal principal,
            HttpServletRequest request
    ) {
        accounts.archive(accounts.account(userId),
                currentUser(principal), request.getRemoteAddr());
        return "redirect:/admin/users?archived";
    }

    /**
     * Reactivate an account (PRG with a {@code reactivated} flag).
     *
     * @param userId    the account to reactivate
     * @param principal the logged-in admin
     * @param request   provides the client IP for the audit trail
     * @return a redirect to the account list
     */
    @PostMapping("/users/{userId}/reactivate")
    public String reactivateAccount(
            @PathVariable UUID userId,
            Principal principal,
            HttpServletRequest request
    ) {
        accounts.reactivate(accounts.account(userId),
                currentUser(principal), request.getRemoteAddr());
        return "redirect:/admin/users?reactivated";
    }

    private User currentUser(Principal principal) {
        return users.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + principal.getName()));
    }
}
