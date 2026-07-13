package com.ericbouchut.learndev.course;

import com.ericbouchut.learndev.course.dto.CourseForm;
import com.ericbouchut.learndev.course.dto.LessonForm;
import com.ericbouchut.learndev.course.entity.Course;
import com.ericbouchut.learndev.course.entity.Lesson;
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

/**
 * <b>Web</b> endpoints of the instructor authoring area under
 * {@code /instructor/**} (gated to {@code ROLE_INSTRUCTOR} by the security
 * rules): the instructor's course list, the course create/edit forms, and
 * the publication lifecycle actions. Ownership and transition rules live in
 * {@link InstructorCourseService}.
 */
@Controller
@RequestMapping("/instructor/courses")
public class InstructorCourseController {

    private final InstructorCourseService instructorCourses;
    private final UserRepository users;

    public InstructorCourseController(
            InstructorCourseService instructorCourses,
            UserRepository users
    ) {
        this.instructorCourses = instructorCourses;
        this.users = users;
    }

    /**
     * Display the instructor's courses, all statuses included.
     *
     * @param principal the logged-in instructor
     * @param model     receives the instructor's courses
     * @return the instructor course list view name
     */
    @GetMapping
    public String myCourses(Principal principal, Model model) {
        model.addAttribute("courses",
                instructorCourses.myCourses(currentUser(principal)));
        return "instructor/courses";
    }

    /**
     * Display the course creation form.
     *
     * @param model receives an empty form
     * @return the course form view name
     */
    @GetMapping("/new")
    public String newCourseForm(Model model) {
        model.addAttribute("form", new CourseForm("", ""));
        model.addAttribute("course", null);
        return "instructor/course-form";
    }

    /**
     * Create a DRAFT course from the submitted form.
     *
     * @param form      the submitted form, validated by {@code @Valid}
     * @param binding   collects validation errors
     * @param principal the logged-in instructor
     * @param model     re-exposes the (null) course on re-render
     * @return a redirect to the edit page, or the re-rendered form on error
     */
    @PostMapping("/new")
    public String createCourse(
            @Valid @ModelAttribute("form") CourseForm form,
            BindingResult binding,
            Principal principal,
            Model model
    ) {
        if (binding.hasErrors()) {
            model.addAttribute("course", null);
            return "instructor/course-form";
        }
        Course course = instructorCourses.create(currentUser(principal), form);
        return "redirect:/instructor/courses/" + course.getCourseId() + "/edit?created";
    }

    /**
     * Display the edit form of one of the instructor's courses.
     *
     * @param courseId  the course to edit
     * @param principal the logged-in instructor (ownership check)
     * @param model     receives the course and its pre-filled form
     * @return the course form view name
     */
    @GetMapping("/{courseId}/edit")
    public String editCourseForm(
            @PathVariable Long courseId,
            Principal principal,
            Model model
    ) {
        Course course = instructorCourses.ownedCourse(courseId, currentUser(principal));
        model.addAttribute("course", course);
        model.addAttribute("lessons", instructorCourses.lessonsOf(course));
        model.addAttribute("form",
                new CourseForm(course.getTitle(), course.getDescription()));
        return "instructor/course-form";
    }

    /**
     * Update the course from the submitted form.
     *
     * @param courseId  the course to update
     * @param form      the submitted form, validated by {@code @Valid}
     * @param binding   collects validation errors
     * @param principal the logged-in instructor (ownership check)
     * @param model     re-exposes the course on re-render
     * @return a redirect to the edit page, or the re-rendered form on error
     */
    @PostMapping("/{courseId}/edit")
    public String updateCourse(
            @PathVariable Long courseId,
            @Valid @ModelAttribute("form") CourseForm form,
            BindingResult binding,
            Principal principal,
            Model model
    ) {
        Course course = instructorCourses.ownedCourse(courseId, currentUser(principal));
        if (binding.hasErrors()) {
            model.addAttribute("course", course);
            return "instructor/course-form";
        }
        instructorCourses.update(course, form);
        return "redirect:/instructor/courses/" + courseId + "/edit?saved";
    }

    /**
     * Publish the course (PRG with a {@code published} flag).
     *
     * @param courseId  the course to publish
     * @param principal the logged-in instructor (ownership check)
     * @param request   provides the client IP for the audit trail
     * @return a redirect to the edit page
     */
    @PostMapping("/{courseId}/publish")
    public String publishCourse(
            @PathVariable Long courseId,
            Principal principal,
            HttpServletRequest request
    ) {
        User instructor = currentUser(principal);
        Course course = instructorCourses.ownedCourse(courseId, instructor);
        instructorCourses.publish(course, instructor, request.getRemoteAddr());
        return "redirect:/instructor/courses/" + courseId + "/edit?published";
    }

    /**
     * Archive the course (PRG with an {@code archived} flag).
     *
     * @param courseId  the course to archive
     * @param principal the logged-in instructor (ownership check)
     * @param request   provides the client IP for the audit trail
     * @return a redirect to the edit page
     */
    @PostMapping("/{courseId}/archive")
    public String archiveCourse(
            @PathVariable Long courseId,
            Principal principal,
            HttpServletRequest request
    ) {
        User instructor = currentUser(principal);
        Course course = instructorCourses.ownedCourse(courseId, instructor);
        instructorCourses.archive(course, instructor, request.getRemoteAddr());
        return "redirect:/instructor/courses/" + courseId + "/edit?archived";
    }

    /**
     * Restore an archived course to draft (PRG with a {@code restored} flag).
     *
     * @param courseId  the course to restore
     * @param principal the logged-in instructor (ownership check)
     * @param request   provides the client IP for the audit trail
     * @return a redirect to the edit page
     */
    @PostMapping("/{courseId}/restore")
    public String restoreCourse(
            @PathVariable Long courseId,
            Principal principal,
            HttpServletRequest request
    ) {
        User instructor = currentUser(principal);
        Course course = instructorCourses.ownedCourse(courseId, instructor);
        instructorCourses.restore(course, instructor, request.getRemoteAddr());
        return "redirect:/instructor/courses/" + courseId + "/edit?restored";
    }

    /**
     * Display the lesson creation form for one of the instructor's courses.
     *
     * @param courseId  the course the lesson will belong to
     * @param principal the logged-in instructor (ownership check)
     * @param model     receives the course and an empty form
     * @return the lesson form view name
     */
    @GetMapping("/{courseId}/lessons/new")
    public String newLessonForm(
            @PathVariable Long courseId,
            Principal principal,
            Model model
    ) {
        Course course = instructorCourses.ownedCourse(courseId, currentUser(principal));
        model.addAttribute("course", course);
        model.addAttribute("lesson", null);
        model.addAttribute("form", new LessonForm("", ""));
        return "instructor/lesson-form";
    }

    /**
     * Create a DRAFT lesson at the end of the course.
     *
     * @param courseId  the course the lesson belongs to
     * @param form      the submitted form, validated by {@code @Valid}
     * @param binding   collects validation errors
     * @param principal the logged-in instructor (ownership check)
     * @param model     re-exposes the course on re-render
     * @return a redirect to the course editor, or the re-rendered form
     */
    @PostMapping("/{courseId}/lessons/new")
    public String createLesson(
            @PathVariable Long courseId,
            @Valid @ModelAttribute("form") LessonForm form,
            BindingResult binding,
            Principal principal,
            Model model
    ) {
        Course course = instructorCourses.ownedCourse(courseId, currentUser(principal));
        if (binding.hasErrors()) {
            model.addAttribute("course", course);
            model.addAttribute("lesson", null);
            return "instructor/lesson-form";
        }
        instructorCourses.createLesson(course, form);
        return "redirect:/instructor/courses/" + courseId + "/edit?lesson-created";
    }

    /**
     * Display the edit form of one lesson.
     *
     * @param courseId  the course the lesson belongs to
     * @param lessonId  the lesson to edit
     * @param principal the logged-in instructor (ownership check)
     * @param model     receives the course, the lesson, and its form
     * @return the lesson form view name
     */
    @GetMapping("/{courseId}/lessons/{lessonId}/edit")
    public String editLessonForm(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            Principal principal,
            Model model
    ) {
        Course course = instructorCourses.ownedCourse(courseId, currentUser(principal));
        Lesson lesson = instructorCourses.ownedLesson(course, lessonId);
        model.addAttribute("course", course);
        model.addAttribute("lesson", lesson);
        model.addAttribute("form",
                new LessonForm(lesson.getTitle(), lesson.getContentMarkdown()));
        return "instructor/lesson-form";
    }

    /**
     * Update the lesson from the submitted form.
     *
     * @param courseId  the course the lesson belongs to
     * @param lessonId  the lesson to update
     * @param form      the submitted form, validated by {@code @Valid}
     * @param binding   collects validation errors
     * @param principal the logged-in instructor (ownership check)
     * @param model     re-exposes the course and lesson on re-render
     * @return a redirect to the course editor, or the re-rendered form
     */
    @PostMapping("/{courseId}/lessons/{lessonId}/edit")
    public String updateLesson(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @Valid @ModelAttribute("form") LessonForm form,
            BindingResult binding,
            Principal principal,
            Model model
    ) {
        Course course = instructorCourses.ownedCourse(courseId, currentUser(principal));
        Lesson lesson = instructorCourses.ownedLesson(course, lessonId);
        if (binding.hasErrors()) {
            model.addAttribute("course", course);
            model.addAttribute("lesson", lesson);
            return "instructor/lesson-form";
        }
        instructorCourses.updateLesson(lesson, form);
        return "redirect:/instructor/courses/" + courseId + "/edit?lesson-saved";
    }

    /**
     * Publish, archive, or restore a lesson (PRG back to the course editor).
     *
     * @param courseId  the course the lesson belongs to
     * @param lessonId  the lesson to transition
     * @param action    one of {@code publish}, {@code archive}, {@code restore}
     * @param principal the logged-in instructor (ownership check)
     * @param request   provides the client IP for the audit trail
     * @return a redirect to the course editor
     */
    @PostMapping("/{courseId}/lessons/{lessonId}/{action:publish|archive|restore}")
    public String transitionLesson(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @PathVariable String action,
            Principal principal,
            HttpServletRequest request
    ) {
        User instructor = currentUser(principal);
        Course course = instructorCourses.ownedCourse(courseId, instructor);
        Lesson lesson = instructorCourses.ownedLesson(course, lessonId);
        String ip = request.getRemoteAddr();
        String flag;
        switch (action) {
            case "publish" -> {
                instructorCourses.publishLesson(lesson, instructor, ip);
                flag = "lesson-published";
            }
            case "archive" -> {
                instructorCourses.archiveLesson(lesson, instructor, ip);
                flag = "lesson-archived";
            }
            default -> {
                instructorCourses.restoreLesson(lesson, instructor, ip);
                flag = "lesson-restored";
            }
        }
        return "redirect:/instructor/courses/" + courseId + "/edit?" + flag;
    }

    /**
     * Move a lesson one step up or down in reading order (PRG).
     *
     * @param courseId  the course the lesson belongs to
     * @param lessonId  the lesson to move
     * @param direction {@code move-up} or {@code move-down}
     * @param principal the logged-in instructor (ownership check)
     * @return a redirect to the course editor
     */
    @PostMapping("/{courseId}/lessons/{lessonId}/{direction:move-up|move-down}")
    public String moveLesson(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @PathVariable String direction,
            Principal principal
    ) {
        Course course = instructorCourses.ownedCourse(courseId, currentUser(principal));
        Lesson lesson = instructorCourses.ownedLesson(course, lessonId);
        if ("move-up".equals(direction)) {
            instructorCourses.moveLessonUp(course, lesson);
        } else {
            instructorCourses.moveLessonDown(course, lesson);
        }
        return "redirect:/instructor/courses/" + courseId + "/edit";
    }

    private User currentUser(Principal principal) {
        return users.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + principal.getName()));
    }
}
