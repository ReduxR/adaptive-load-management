package com.reduxr.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class ReleaseDateValidator implements ConstraintValidator<ValidReleaseDate, LocalDate> {
    private LocalDate minimumDate;

    @Override
    public void initialize(ValidReleaseDate annotation) {
        minimumDate = LocalDate.of(annotation.minYear(), 1, 1);
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        return value != null && !value.isBefore(minimumDate);
    }
}
