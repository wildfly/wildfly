/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.util;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;


/**
 *
 * @author Stuart Douglas
 *
 */
public class Reflections {

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object obj) {
        return (T) obj;
    }

    public static <T> T newInstance(String className, ClassLoader classLoader) {
        try {
            Class<?> clazz = classLoader.loadClass(className);
            return (T) clazz.newInstance();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isAccessible(String className, ClassLoader classLoader) {
        return loadClass(className, classLoader) != null;
    }

    public static <T> Class<T> loadClass(String className, ClassLoader classLoader) {
        try {
            return cast(classLoader.loadClass(className));
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * A simple implementation of the {@link org.jboss.weld.resources.spi.AnnotationDiscovery#containsAnnotation(Class, Class)} contract.
     * This implementation uses reflection.
     *
     * @see org.jboss.as.weld.discovery.WeldAnnotationDiscovery
     */
    public static boolean containsAnnotation(Class<?> javaClass, Class<? extends Annotation> requiredAnnotation) {
        for (Class<?> clazz = javaClass; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            // class level annotations
            if (clazz == javaClass || requiredAnnotation.isAnnotationPresent(Inherited.class)) {
                if (containsAnnotations(clazz.getAnnotations(), requiredAnnotation)) {
                    return true;
                }
            }
            // fields
            for (Field field : clazz.getDeclaredFields()) {
                if (containsAnnotations(field.getAnnotations(), requiredAnnotation)) {
                    return true;
                }
            }
            // constructors
            for (Constructor<?> constructor : clazz.getConstructors()) {
                if (containsAnnotations(constructor.getAnnotations(), requiredAnnotation)) {
                    return true;
                }
                for (Annotation[] parameterAnnotations : constructor.getParameterAnnotations()) {
                    if (containsAnnotations(parameterAnnotations, requiredAnnotation)) {
                        return true;
                    }
                }
            }
            // methods
            for (Method method : clazz.getDeclaredMethods()) {
                if (containsAnnotations(method.getAnnotations(), requiredAnnotation)) {
                    return true;
                }
                for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
                    if (containsAnnotations(parameterAnnotations, requiredAnnotation)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean containsAnnotations(Annotation[] annotations, Class<? extends Annotation> requiredAnnotation) {
        return containsAnnotation(annotations, requiredAnnotation, true);
    }

    private static boolean containsAnnotation(Annotation[] annotations, Class<? extends Annotation> requiredAnnotation, boolean checkMetaAnnotations) {
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (requiredAnnotation.equals(annotationType)) {
                return true;
            }
            if (checkMetaAnnotations && containsAnnotation(annotationType.getAnnotations(), requiredAnnotation, false)) {
                return true;
            }
        }
        return false;
    }

}
