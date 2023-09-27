/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.validator.cdi;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * A validator which makes use of constructor injection.
 *
 * @author Gunnar Morling
 */
public class CustomMaxValidator implements ConstraintValidator<CustomMax, Integer> {

    private final MaximumValueProvider maximumValueProvider;

    @Inject
    public CustomMaxValidator(MaximumValueProvider maximumValueProvider) {
        this.maximumValueProvider = maximumValueProvider;
    }

    @Override
    public void initialize(CustomMax constraintAnnotation) {
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value <= maximumValueProvider.getMax();
    }
}
