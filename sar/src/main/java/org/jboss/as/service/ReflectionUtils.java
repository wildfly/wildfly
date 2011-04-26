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
import java.util.Iterator;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;

/**
 * Reflection utility methods.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ReflectionUtils {

    private ReflectionUtils() {
        // forbidden instantiation
    }

    static Method getGetter(final ClassReflectionIndex<?> classIndex, final String propertyName) {
        final String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        final String iserName = "is" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
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

        throw new IllegalStateException("No such get method for property '" + propertyName + "' found on " + classIndex.getIndexedClass());
    }

    static Method getSetter(final ClassReflectionIndex<?> classIndex, final String propertyName) {
        final String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);

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

        throw new IllegalStateException("No such set method for property '" + propertyName + "' found on " + classIndex.getIndexedClass());
    }

    static Method getMethod(final ClassReflectionIndex<?> classIndex, final String methodName, final Class<?>[] types, final boolean fail) {
        final Collection<Method> methods = classIndex.getMethods(methodName, types);
        if (methods.size() == 1) {
            return methods.iterator().next();
        }
        if (fail) {
            throw new IllegalStateException(noSuchMethod(classIndex, methodName, types));
        } else {
            return null;
        }
    }

    static Object newInstance(final Constructor<?> constructor, final Object[] args) throws DeploymentUnitProcessingException {
        try {
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException("Component class not instantiated", e);
        }
    }

    static Class<?> getClass(final String className, final ClassLoader classLoader) throws DeploymentUnitProcessingException {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new DeploymentUnitProcessingException("Component class not found", e);
        }
    }

    private static String noSuchMethod(final ClassReflectionIndex<?> classIndex, final String methodName, final Class<?>[] parameterTypes) {
        StringBuffer message = new StringBuffer();
        message.append("No such method '");
        message.append(methodName);
        appendParameterList(message, parameterTypes);
        message.append("' found on ").append(classIndex.getIndexedClass());
        return message.toString();
    }

    private static void appendParameterList(final StringBuffer stringBuffer, final Class<?>[] parameterTypes) {
        stringBuffer.append('(');
        if (parameterTypes != null && parameterTypes.length > 0) {
            stringBuffer.append(parameterTypes[0]);
            for (int i = 1; i < parameterTypes.length; i++) {
                stringBuffer.append(", ").append(parameterTypes[i]);
            }
        }
        stringBuffer.append(')');
    }

}
