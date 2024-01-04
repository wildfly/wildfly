/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.beanvalidation.hibernate.validator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * A custom constraint which expects a custom constraint validator factory.
 *
 * @author Gunnar Morling
 */
@Constraint(validatedBy = CustomConstraint.Validator.class)
@Documented
@Target(FIELD)
@Retention(RUNTIME)
public @interface CustomConstraint {

    String message() default "my custom constraint";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public class Validator implements ConstraintValidator<CustomConstraint, String> {

        private final String message;

        public Validator() {
            this.message = "Created by default factory";
        }

        public Validator(String message) {
            this.message = message;
        }

        @Override
        public void initialize(CustomConstraint parameters) {
        }

        @Override
        public boolean isValid(String object, ConstraintValidatorContext constraintValidatorContext) {
            if (object == null) {
                return true;
            }

            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate(message).addConstraintViolation();

            return false;
        }
    }
}
