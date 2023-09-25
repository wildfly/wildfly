/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.validator.cdi;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Custom constraint using CDI in its validator class.
 *
 * @author Gunnar Morling
 */
@Constraint(validatedBy = CustomMaxValidator.class)
@Documented
@Target({ METHOD, FIELD, TYPE, PARAMETER })
@Retention(RUNTIME)
public @interface CustomMax {
    String message() default "my custom constraint";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
