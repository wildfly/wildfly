/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ee.beanvalidation.testprovider;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;
import javax.validation.metadata.BeanDescriptor;

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
