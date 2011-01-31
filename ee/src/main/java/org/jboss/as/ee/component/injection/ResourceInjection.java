/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component.injection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

/**
 * An instance of an injection.  This can be called at any time against an object instance to actually perform the
 * injection.
 *
 * @author John Bailey
 */
public interface ResourceInjection {
    /**
     * Run this resource injection on the target instance.
     *
     * @param target The target object to inject
     */
    void inject(final Object target);

    /**
     * Run this resource un-injection on the target instance.
     *
     * @param target The target object to inject
     */
    void uninject(final Object target);

    /**
     * Factory to create injections
     */
    class Factory {
        /**
         * Create the correct injection instance.
         *
         * @param resourceConfiguration The resource injection configuration
         * @param beanClass The bean class to injection should run against.
         * @param value The value for injection
         * @param <V> The value type
         * @return The injection instance
         */
        public static <V> ResourceInjection create(final ResourceInjectionConfiguration resourceConfiguration, final Class<?> beanClass, Value<V> value) {
            final Class<?> argClass;
            try {
                argClass = beanClass.getClassLoader().loadClass(resourceConfiguration.getInjectedType());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Invalid resource injection configuration.", e);
            }
            switch(resourceConfiguration.getTargetType()) {
                case FIELD: {
                    final Field field;
                    try {
                        field = beanClass.getDeclaredField(resourceConfiguration.getName());
                        field.setAccessible(true);
                    } catch (NoSuchFieldException e) {
                        throw new IllegalArgumentException("Invalid resource injection configuration.  Field is not found on target class.", e);
                    }
                    return new FieldResourceInjection<V>(Values.immediateValue(field), value, argClass.isPrimitive());
                }
                case METHOD: {
                    final Method method;
                    try {
                        method = beanClass.getDeclaredMethod(resourceConfiguration.getName(), argClass);
                        method.setAccessible(true);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid resource injection configuration.  Method could not be retrieved from target class", e);
                    }
                    return new MethodResourceInjection<V>(Values.immediateValue(method), value, argClass.isPrimitive());
                }
                default:
                    return null;
            }
        }
    }
}
