package com.ericbouchut.learndev.course;

import com.ericbouchut.learndev.course.entity.Course;
import com.ericbouchut.learndev.course.entity.Enrollment;
import com.ericbouchut.learndev.course.entity.EnrollmentStatus;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <b>Web</b> endpoints of the student course experience: the published
 * course catalogue, the course detail page, and the enroll/drop actions.
 * Access requires authentication ({@code /courses/**} in the security
 * rules); visibility of individual courses is decided by
 * {@link CourseService} and the enrollment lifecycle by
 * {@link EnrollmentService}.
 */
@Controller
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final UserRepository users;

    public CourseController(
            CourseService courseService,
            EnrollmentService enrollmentService,
            UserRepository users
    ) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.users = users;
    }

    /**
     * Display the catalogue of published courses, with the logged-in
     * student's active enrollments marked.
     *
     * @param principal the logged-in student
     * @param model     receives the courses and the enrolled course ids
     * @return the catalogue view name
     */
    @GetMapping("/courses")
    public String catalogue(Principal principal, Model model) {
        Map<Long, EnrollmentStatus> enrollmentByCourseId =
                enrollmentService.myEnrollments(currentUser(principal)).stream()
                        .filter(e -> e.getStatus() != EnrollmentStatus.DROPPED)
                        .collect(Collectors.toMap(
                                e -> e.getCourse().getCourseId(),
                                Enrollment::getStatus));
        model.addAttribute("courses", courseService.catalogue());
        model.addAttribute("enrollmentByCourseId", enrollmentByCourseId);
        return "courses/catalog";
    }

    /**
     * Display one course with its published lessons and the enroll or quit
     * action matching the student's enrollment state.
     *
     * @param courseId  the course to show
     * @param principal the logged-in student (drives visibility)
     * @param model     receives the course, its readable lessons, and the
     *                  student's enrollment (null when never enrolled)
     * @return the course detail view name
     */
    @GetMapping("/courses/{courseId}")
    public String detail(@PathVariable Long courseId, Principal principal, Model model) {
        User student = currentUser(principal);
        Course course = courseService.visibleCourse(courseId, student);
        model.addAttribute("course", course);
        model.addAttribute("lessons", courseService.publishedLessons(course));
        model.addAttribute("enrollment",
                enrollmentService.enrollmentFor(student, course).orElse(null));
        return "courses/detail";
    }

    /**
     * Register the student in the course, then redirect back to the course
     * page with an {@code enrolled} confirmation flag (PRG).
     *
     * @param courseId  the course to join
     * @param principal the logged-in student
     * @return a redirect to the course detail page
     */
    @PostMapping("/courses/{courseId}/enroll")
    public String enroll(@PathVariable Long courseId, Principal principal) {
        User student = currentUser(principal);
        Course course = courseService.visibleCourse(courseId, student);
        enrollmentService.enroll(student, course);
        return "redirect:/courses/" + courseId + "?enrolled";
    }

    /**
     * Drop the student from the course, then redirect back to the course
     * page with a {@code dropped} confirmation flag (PRG).
     *
     * @param courseId  the course to leave
     * @param principal the logged-in student
     * @return a redirect to the course detail page
     */
    @PostMapping("/courses/{courseId}/drop")
    public String drop(@PathVariable Long courseId, Principal principal) {
        User student = currentUser(principal);
        Course course = courseService.visibleCourse(courseId, student);
        enrollmentService.drop(student, course);
        return "redirect:/courses/" + courseId + "?dropped";
    }

    private User currentUser(Principal principal) {
        return users.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + principal.getName()));
    }
}
