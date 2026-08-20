package com.ericbouchut.learndev.admin;

import com.ericbouchut.learndev.auth.dto.RegisterForm;
import com.ericbouchut.learndev.auth.exception.DuplicateEmailException;
import com.ericbouchut.learndev.auth.exception.DuplicateUsernameException;
import com.ericbouchut.learndev.course.InstructorCourseService;
import com.ericbouchut.learndev.course.entity.Course;
import com.ericbouchut.learndev.course.entity.Lesson;
import com.ericbouchut.learndev.course.repository.CourseRepository;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final CourseRepository courses;
    private final InstructorCourseService instructorCourses;

    public AdminController(
            AccountAdminService accounts,
            UserRepository users,
            CourseRepository courses,
            InstructorCourseService instructorCourses
    ) {
        this.accounts = accounts;
        this.users = users;
        this.courses = courses;
        this.instructorCourses = instructorCourses;
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
     * Unlock an account locked by failed logins (PRG with an
     * {@code unlocked} flag).
     *
     * @param userId    the account to unlock
     * @param principal the logged-in admin
     * @param request   provides the client IP for the audit trail
     * @return a redirect to the account list
     */
    @PostMapping("/users/{userId}/unlock")
    public String unlockAccount(
            @PathVariable UUID userId,
            Principal principal,
            HttpServletRequest request
    ) {
        accounts.unlock(accounts.account(userId),
                currentUser(principal), request.getRemoteAddr());
        return "redirect:/admin/users?unlocked";
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

    /**
     * Display every course, any status, any instructor, with its lessons
     * (content moderation view).
     *
     * @param model receives the courses and their lessons
     * @return the course moderation view name
     */
    @GetMapping("/courses")
    public String courseList(Model model) {
        List<Course> allCourses = courses.findAll(Sort.by("title"));
        Map<Long, List<Lesson>> lessonsByCourseId = new HashMap<>();
        for (Course course : allCourses) {
            lessonsByCourseId.put(course.getCourseId(), instructorCourses.lessonsOf(course));
        }
        model.addAttribute("courses", allCourses);
        model.addAttribute("lessonsByCourseId", lessonsByCourseId);
        return "admin/courses";
    }

    /**
     * Archive or restore any course, bypassing the instructor ownership
     * rule (the lifecycle guards still apply). PRG back to the list.
     *
     * @param courseId  the course to transition
     * @param action    {@code archive} or {@code restore}
     * @param principal the logged-in admin
     * @param request   provides the client IP for the audit trail
     * @return a redirect to the course moderation view
     */
    @PostMapping("/courses/{courseId}/{action:archive|restore}")
    public String transitionCourse(
            @PathVariable Long courseId,
            @PathVariable String action,
            Principal principal,
            HttpServletRequest request
    ) {
        User admin = currentUser(principal);
        Course course = courses.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if ("archive".equals(action)) {
            instructorCourses.archive(course, admin, request.getRemoteAddr());
        } else {
            instructorCourses.restore(course, admin, request.getRemoteAddr());
        }
        return "redirect:/admin/courses?" + action + "d";
    }

    /**
     * Archive or restore any lesson of a course, bypassing the instructor
     * ownership rule. PRG back to the list.
     *
     * @param courseId  the course the lesson belongs to
     * @param lessonId  the lesson to transition
     * @param action    {@code archive} or {@code restore}
     * @param principal the logged-in admin
     * @param request   provides the client IP for the audit trail
     * @return a redirect to the course moderation view
     */
    @PostMapping("/courses/{courseId}/lessons/{lessonId}/{action:archive|restore}")
    public String transitionLesson(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @PathVariable String action,
            Principal principal,
            HttpServletRequest request
    ) {
        User admin = currentUser(principal);
        Course course = courses.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Lesson lesson = instructorCourses.ownedLesson(course, lessonId);
        if ("archive".equals(action)) {
            instructorCourses.archiveLesson(lesson, admin, request.getRemoteAddr());
        } else {
            instructorCourses.restoreLesson(lesson, admin, request.getRemoteAddr());
        }
        return "redirect:/admin/courses?lesson-" + action + "d";
    }

    private User currentUser(Principal principal) {
        return users.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + principal.getName()));
    }
}
