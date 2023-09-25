/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.service.logging.SarLogger;

/**
 * Reflection utility methods.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ReflectionUtils {

    private static final Class<?>[] NO_ARGS = new Class<?>[0];

    private ReflectionUtils() {
        // forbidden instantiation
    }

    static Method getGetter(final Class<?> clazz, final String propertyName) {
        final String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        final String iserName = "is" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);

        try {
            return clazz.getMethod(getterName, NO_ARGS);
        } catch (NoSuchMethodException e) {
            // ignore for now - might be a boolean property
        }
        try {
            return clazz.getMethod(iserName, NO_ARGS);
        } catch (NoSuchMethodException e) {
            final String className = clazz.getName();
            throw SarLogger.ROOT_LOGGER.propertyMethodNotFound("Get", propertyName, className);
        }
    }

    static Method getSetter(final List<ClassReflectionIndex> classHierarchy, final String propertyName) {
        final String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);

        for (final ClassReflectionIndex classIndex : classHierarchy) {
            final Iterator<Method> methods = classIndex.getMethods().iterator();
            Method method;
            while (methods.hasNext()) {
                method = methods.next();
                if (setterName.equals(method.getName()) && method.getParameterCount() == 1) {
                    return method;
                }
            }
        }

        final String className = classHierarchy.get(0).getIndexedClass().getName();
        throw SarLogger.ROOT_LOGGER.propertyMethodNotFound("Set", propertyName, className);
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>[] argumentList) {
        try {
            return clazz.getMethod(methodName, argumentList);
        } catch (NoSuchMethodException e) {
            throw SarLogger.ROOT_LOGGER.methodNotFound(methodName, parameterList(argumentList), clazz.getName());
        }
    }

    static Method getNoArgMethod(final List<ClassReflectionIndex> classHierarchy, final String methodName) {
        for (final ClassReflectionIndex classIndex : classHierarchy) {
            final Collection<Method> methods = classIndex.getMethods(methodName, NO_ARGS);
            if (methods.size() == 1) {
                return methods.iterator().next();
            }
        }

        return null;
    }

    static Object newInstance(final Constructor<?> constructor, final Object[] args) {
        try {
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw SarLogger.ROOT_LOGGER.classNotInstantiated(e);
        }
    }

    static Class<?> getClass(final String className, final ClassLoader classLoader) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (final ClassNotFoundException e) {
            throw SarLogger.ROOT_LOGGER.classNotFound(e);
        }
    }

    private static String parameterList(final Class<?>[] parameterTypes) {
        final StringBuilder result = new StringBuilder();
        if (parameterTypes != null && parameterTypes.length > 0) {
            result.append(parameterTypes[0]);
            for (int i = 1; i < parameterTypes.length; i++) {
                result.append(", ").append(parameterTypes[i]);
            }
        }
        return result.toString();
    }

}
