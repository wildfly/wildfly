/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Bean info API.
 * It checks the whole bean's class hierarchy.
 *
 * @param <T> the exact bean type
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface BeanInfo<T> {
    /**
     * Get ctor; exact match wrt parameter types.
     *
     * @param parameterTypes the parameter types
     * @return the found ctor
     */
    Constructor<T> getConstructor(String... parameterTypes);

    /**
     * Find ctor.
     * Loose parameter type matching; not all types need to be known.
     *
     * @param parameterTypes the parameter types
     * @return the found ctor
     */
    Constructor<T> findConstructor(String... parameterTypes);

    /**
     * Get method; exact match wrt parameter types.
     *
     * @param name the method name
     * @param parameterTypes the parameter types
     * @return found method
     */
    Method getMethod(String name, String... parameterTypes);

    /**
     * Find method.
     * Loose parameter type matching; not all types need to be known.
     *
     * @param name the method name
     * @param parameterTypes the parameter types
     * @return found method
     */
    Method findMethod(String name, String... parameterTypes);

    /**
     * Get getter.
     *
     * @param propertyName the getter propertyName
     * @param type the type propertyName
     * @return the found getter
     */
    Method getGetter(String propertyName, Class<?> type);

    /**
     * Get setter.
     *
     * @param propertyName the setter propertyName
     * @param type the type propertyName
     * @return the found setter
     */
    Method getSetter(String propertyName, Class<?> type);

    /**
     * Get bean's field.
     *
     * @param name the field name
     * @return the found field
     */
    Field getField(String name);
}
