/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.beanvalidation.testprovider;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.BeanDescriptor;

/**
 * A {@link Validator} implementation for testing purposes.
 *
 * @author Gunnar Morling
 */
public class MyValidatorImpl implements Validator {

    @Override
    public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value,
            Class<?>... groups) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ExecutableValidator forExecutables() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
