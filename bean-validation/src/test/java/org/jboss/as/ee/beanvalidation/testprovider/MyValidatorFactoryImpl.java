/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.beanvalidation.testprovider;

import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.ParameterNameProvider;
import jakarta.validation.TraversableResolver;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorContext;
import jakarta.validation.ValidatorFactory;

/**
 * A {@link ValidatorFactory} implementation for testing purposes.
 *
 * @author Gunnar Morling
 */
public class MyValidatorFactoryImpl implements ValidatorFactory {

    @Override
    public Validator getValidator() {
        return new MyValidatorImpl();
    }

    @Override
    public ValidatorContext usingContext() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public MessageInterpolator getMessageInterpolator() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public TraversableResolver getTraversableResolver() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ParameterNameProvider getParameterNameProvider() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ClockProvider getClockProvider() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
