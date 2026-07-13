package com.ericbouchut.learndev.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form-backing record for creating or editing a course
 * (the description is optional; the column is TEXT, no length cap).
 */
public record CourseForm(

        @NotBlank
        @Size(max = 255)
        String title,

        String description
) {
}
