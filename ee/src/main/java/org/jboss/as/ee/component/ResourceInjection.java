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

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.msc.value.Value;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
         * @param resourceConfiguration     The resource injection configuration
         * @param beanClass                 The bean class to injection should run against.
         * @param deploymentReflectionIndex The deployment reflection index
         * @param value                     The value for injection
         * @return The injection instance
         */
        public static ResourceInjection create(final InjectionTargetDescription resourceConfiguration, final Class<?> beanClass, final DeploymentReflectionIndex deploymentReflectionIndex, final Value<ManagedReferenceFactory> value) {
            final Class<?> argClass;
            try {
                argClass = beanClass.getClassLoader().loadClass(resourceConfiguration.getValueClassName());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Invalid resource injection configuration.", e);
            }
            final ClassReflectionIndex<?> classReflectionIndex = deploymentReflectionIndex.getClassIndex(beanClass);
            final String memberName = resourceConfiguration.getName();
            switch (resourceConfiguration.getType()) {
                case FIELD: {
                    final Field field = findField(deploymentReflectionIndex, classReflectionIndex, memberName);
                    if (field == null) {
                        throw new IllegalArgumentException("Field not found - Invalid injection into field '" + memberName + "' of " + beanClass);
                    }
                    if (!field.getType().isAssignableFrom(argClass)) {
                        throw new IllegalArgumentException("Field type " + field.getType() + " is not assignable from " + argClass + " , injection into class " + beanClass + " failed");
                    }
                    return new FieldResourceInjection(field, value);
                }
                case METHOD: {
                    final Method method = findMethod(deploymentReflectionIndex, classReflectionIndex, void.class, memberName, argClass);
                    if (method == null) {
                        throw new IllegalArgumentException("Invalid injection - Method void " + memberName + "(" + argClass.getName() + ")" + " not found on " + beanClass);
                    }
                    if (!method.getParameterTypes()[0].isAssignableFrom(argClass)) {
                        throw new IllegalArgumentException("Field type " + method.getParameterTypes()[0] + " is not assignable from " + argClass + " , injection into class " + beanClass + " failed");
                    }
                    return new MethodResourceInjection(method, value);
                }
                default: {
                    throw new IllegalArgumentException("Resource injection is allowed only on field and method types. Can't handle " + resourceConfiguration.getType());
                }
            }
        }

        /**
         * Finds and returns a field named <code>fieldName</code> from the passed <code>classReflectionIndex</code> or any super class(es)
         * of the {@link Class} corresponding to the passed <code>classReflectionIndex</code>.
         * <p/>
         * Returns null if no such field is found.
         *
         * @param deploymentReflectionIndex The deployment reflection index
         * @param classReflectionIndex      The class reflection index which will be used to traverse the class hierarchy to find the field
         * @param fieldName                 The name of the field
         * @return
         */
        private static Field findField(DeploymentReflectionIndex deploymentReflectionIndex, ClassReflectionIndex<?> classReflectionIndex, String fieldName) {

            final Field field = classReflectionIndex.getField(fieldName);
            if (field != null) {
                return field;
            }
            // find in super class
            Class<?> superClass = classReflectionIndex.getIndexedClass().getSuperclass();
            if (superClass != null) {
                ClassReflectionIndex<?> superClassIndex = deploymentReflectionIndex.getClassIndex(superClass);
                if (superClassIndex != null) {
                    return findField(deploymentReflectionIndex, superClassIndex, fieldName);
                }
            }
            return null;
        }

        /**
         * Finds and returns a method named <code>methodName</code> which accepts the passed <code>paramTypes</code> and whose
         * return type is the passed <code>returnType</code>. The passed <code>classReflectionIndex</code> will be used to traverse
         * the class hierarchy while finding the method.
         * <p/>
         * Returns null if no such method is found.
         *
         * @param deploymentReflectionIndex The deployment reflection index
         * @param classReflectionIndex      The class reflection index which will be used to traverse the class hierarchy to find the method
         * @param returnType                The return type of the method being searched
         * @param methodName                The name of the method
         * @param paramTypes                The param types of the method being searched
         * @return
         */
        private static Method findMethod(DeploymentReflectionIndex deploymentReflectionIndex, ClassReflectionIndex<?> classReflectionIndex, Class<?> returnType, String methodName, Class<?>... paramTypes) {
            Method method = classReflectionIndex.getMethod(returnType, methodName, paramTypes);
            if (method != null) {
                return method;
            }
            // find in super class
            Class<?> superClass = classReflectionIndex.getIndexedClass().getSuperclass();
            if (superClass != null) {
                ClassReflectionIndex<?> superClassIndex = deploymentReflectionIndex.getClassIndex(superClass);
                if (superClassIndex != null) {
                    return findMethod(deploymentReflectionIndex, superClassIndex, returnType, methodName, paramTypes);
                }
            }
            return null;
        }
    }
}
