package com.ericbouchut.learndev.course;

import com.ericbouchut.learndev.course.entity.EnrollmentStatus;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

/**
 * <b>Web</b> endpoint of the student dashboard: the logged-in user's active
 * courses with their enrollment status. Lives in the course package because
 * the page renders enrollment data (the auth package only brings the user
 * here after login).
 */
@Controller
public class DashboardController {

    private final EnrollmentService enrollmentService;
    private final UserRepository users;

    public DashboardController(EnrollmentService enrollmentService, UserRepository users) {
        this.enrollmentService = enrollmentService;
        this.users = users;
    }

    /**
     * Display the dashboard with the student's active enrollments
     * (dropped ones are not listed; re-enrolling brings them back).
     *
     * @param principal the logged-in user
     * @param model     receives the active enrollments
     * @return the dashboard view name
     */
    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        User user = users.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + principal.getName()));
        model.addAttribute("enrollments",
                enrollmentService.myEnrollments(user).stream()
                        .filter(e -> e.getStatus() != EnrollmentStatus.DROPPED)
                        .toList());
        return "dashboard";
    }
}
