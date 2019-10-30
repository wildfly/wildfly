/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
