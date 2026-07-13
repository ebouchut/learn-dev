package com.ericbouchut.learndev.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form-backing record for creating or editing a lesson. The content is
 * Markdown, optional while drafting (the column is TEXT with a '' default);
 * the reading position is managed by the service (move up/down), not typed
 * in by the instructor.
 */
public record LessonForm(

        @NotBlank
        @Size(max = 255)
        String title,

        String contentMarkdown
) {
}
