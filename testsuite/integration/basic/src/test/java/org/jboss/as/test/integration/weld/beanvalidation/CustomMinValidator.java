/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.beanvalidation;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * A validator that uses constructor injection.
 *
 * @author Farah Juma
 */
public class CustomMinValidator implements ConstraintValidator<CustomMin, Integer> {

    private final MinimumValueProvider minimumValueProvider;

    @Inject
    public CustomMinValidator(MinimumValueProvider minimumValueProvider) {
        this.minimumValueProvider = minimumValueProvider;
    }

    @Override
    public void initialize(CustomMin constraintAnnotation) {
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value >= minimumValueProvider.getMin();
    }
}
