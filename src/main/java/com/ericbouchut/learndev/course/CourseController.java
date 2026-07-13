package com.ericbouchut.learndev.course;

import com.ericbouchut.learndev.course.entity.Course;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;

/**
 * <b>Web</b> endpoints of the student course experience: the published
 * course catalogue and the course detail page. Access requires
 * authentication ({@code /courses/**} in the security rules); visibility of
 * individual courses is decided by {@link CourseService}.
 */
@Controller
public class CourseController {

    private final CourseService courseService;
    private final UserRepository users;

    public CourseController(CourseService courseService, UserRepository users) {
        this.courseService = courseService;
        this.users = users;
    }

    /**
     * Display the catalogue of published courses.
     *
     * @param model receives the published courses
     * @return the catalogue view name
     */
    @GetMapping("/courses")
    public String catalogue(Model model) {
        model.addAttribute("courses", courseService.catalogue());
        return "courses/catalog";
    }

    /**
     * Display one course with its published lessons.
     *
     * @param courseId  the course to show
     * @param principal the logged-in student (drives visibility)
     * @param model     receives the course and its readable lessons
     * @return the course detail view name
     */
    @GetMapping("/courses/{courseId}")
    public String detail(@PathVariable Long courseId, Principal principal, Model model) {
        Course course = courseService.visibleCourse(courseId, currentUser(principal));
        model.addAttribute("course", course);
        model.addAttribute("lessons", courseService.publishedLessons(course));
        return "courses/detail";
    }

    private User currentUser(Principal principal) {
        return users.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + principal.getName()));
    }
}
