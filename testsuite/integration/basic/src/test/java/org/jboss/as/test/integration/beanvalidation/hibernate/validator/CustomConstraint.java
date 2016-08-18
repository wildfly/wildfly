/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.beanvalidation.hibernate.validator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

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