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

package org.jboss.as.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.service.logging.SarLogger;

/**
 * Reflection utility methods.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ReflectionUtils {

    private ReflectionUtils() {
        // forbidden instantiation
    }

    static Method getGetter(final Class<?> clazz, final String propertyName) {
        final String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        final String iserName = "is" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);

        try {
            return clazz.getMethod(getterName, new Class[]{});
        } catch (NoSuchMethodException e) {
            // ignore for now - might be a boolean property
        }
        try {
            return clazz.getMethod(iserName, new Class[]{});
        } catch (NoSuchMethodException e) {
            final String className = clazz.getName();
            throw SarLogger.ROOT_LOGGER.propertyMethodNotFound("Get", propertyName, className);
        }
    }

    static Method getGetter(final List<ClassReflectionIndex> classHierarchy, final String propertyName) {
        final String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        final String iserName = "is" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);

        for (final ClassReflectionIndex classIndex : classHierarchy) {
            final Iterator<Method> methods = classIndex.getMethods().iterator();
            Method method = null;
            String methodName = null;
            while (methods.hasNext()) {
                method = methods.next();
                methodName = method.getName();
                if ((getterName.equals(methodName) || iserName.equals(methodName)) && method.getParameterTypes().length == 0) {
                    return method;
                }
            }
        }

        final String className = classHierarchy.get(0).getIndexedClass().getName();
        throw SarLogger.ROOT_LOGGER.propertyMethodNotFound("Get", propertyName, className);
    }

    static Method getSetter(final List<ClassReflectionIndex> classHierarchy, final String propertyName) {
        final String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);

        for (final ClassReflectionIndex classIndex : classHierarchy) {
            final Iterator<Method> methods = classIndex.getMethods().iterator();
            Method method = null;
            String methodName = null;
            while (methods.hasNext()) {
                method = methods.next();
                methodName = method.getName();
                if (setterName.equals(methodName) && method.getParameterTypes().length == 1) {
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

    static Method getMethod(final List<ClassReflectionIndex> classHierarchy, final String methodName, final Class<?>[] types, final boolean fail) {
        for (final ClassReflectionIndex classIndex : classHierarchy) {
            final Collection<Method> methods = classIndex.getMethods(methodName, types);
            if (methods.size() == 1) {
                return methods.iterator().next();
            }
        }
        if (fail) {
            final String className = classHierarchy.get(0).getIndexedClass().getName();
            throw SarLogger.ROOT_LOGGER.methodNotFound(methodName, parameterList(types), className);
        } else {
            return null;
        }
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

    static List<ClassReflectionIndex> getClassHierarchy(final String className, final DeploymentReflectionIndex index, final ClassLoader classLoader) {
        final List<ClassReflectionIndex> retVal = new LinkedList<ClassReflectionIndex>();

        Class<?> temp = getClass(className, classLoader);
        while (temp != null) {
            retVal.add(index.getClassIndex(temp));
            temp = temp.getSuperclass();
        }

        return Collections.unmodifiableList(retVal);
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
