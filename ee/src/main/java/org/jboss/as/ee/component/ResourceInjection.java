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

package org.jboss.as.ee.component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.jboss.as.naming.JndiInjectable;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.msc.value.Value;

/**
 * An instance of an injection.  This can be called at any time against an object instance to actually perform the
 * injection.
 *
 * @author John Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ResourceInjection {
    /**
     * Run this resource injection on the target instance.
     *
     * @param target the target object to inject into
     */
    void inject(final Object target);

    /**
     * Factory to create injections.
     */
    class Factory {

        private Factory() {
        }

        /**
         * Create the correct injection instance.
         *
         * @param resourceConfiguration The resource injection configuration
         * @param beanClass The bean class to injection should run against.
         * @param reflectionIndex The class reflection index
         * @param value The value for injection
         * @return The injection instance
         */
        public static ResourceInjection create(final InjectionTargetDescription resourceConfiguration, final Class<?> beanClass, final ClassReflectionIndex<?> reflectionIndex, final Value<JndiInjectable> value) {
            final Class<?> argClass;
            try {
                argClass = beanClass.getClassLoader().loadClass(resourceConfiguration.getValueClassName());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Invalid resource injection configuration.", e);
            }
            final String memberName = resourceConfiguration.getName();
            switch(resourceConfiguration.getType()) {
                case FIELD: {
                    final Field field = reflectionIndex.getField(memberName);
                    if (field == null || ! field.getType().isAssignableFrom(argClass)) {
                        throw new IllegalArgumentException("Invalid injection into field '" + memberName + "' of " + beanClass);
                    }
                    return new FieldResourceInjection(field, value);
                }
                case METHOD: {
                    final Method method = reflectionIndex.getMethod(void.class, memberName, argClass);
                    if (method == null || ! method.getParameterTypes()[0].isAssignableFrom(argClass)) {
                        throw new IllegalArgumentException("Invalid injection into method '" + memberName + "' of " + beanClass);
                    }
                    return new MethodResourceInjection(method, value);
                }
                default: {
                    throw new IllegalStateException();
                }
            }
        }
    }
}
