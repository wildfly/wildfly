/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.protostream.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;

import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Utility methods requiring privileged actions for use by reflection-based marshallers.
 * Do not change class/method visibility to avoid being called from other {@link java.security.CodeSource}s, thus granting privilege escalation to external code.
 * @author Paul Ferraro
 */
final class Reflect {

    static Field findField(Class<?> sourceClass, Class<?> fieldType) {
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<Field>() {
            @Override
            public Field run() {
                for (Field field : sourceClass.getDeclaredFields()) {
                    if (field.getType().isAssignableFrom(fieldType)) {
                        field.setAccessible(true);
                        return field;
                    }
                }
                Class<?> superClass = sourceClass.getSuperclass();
                if ((superClass == null) || (superClass == Object.class)) {
                    throw new IllegalArgumentException(fieldType.getName());
                }
                return findField(superClass, fieldType);
            }
        });
    }

    static Method findMethod(Class<?> sourceClass, Class<?> returnType) {
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<Method>() {
            @Override
            public Method run() {
                for (Method method : sourceClass.getDeclaredMethods()) {
                    if ((method.getParameterCount() == 0) && (method.getReturnType() == returnType)) {
                        method.setAccessible(true);
                        return method;
                    }
                }
                Class<?> superClass = sourceClass.getSuperclass();
                if ((superClass == null) || (superClass == Object.class)) {
                    throw new IllegalArgumentException(returnType.getName());
                }
                return findMethod(superClass, returnType);
            }
        });
    }

    static Method findMethod(Class<?> sourceClass, String methodName) {
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<Method>() {
            @Override
            public Method run() {
                try {
                    Method method = sourceClass.getDeclaredMethod(methodName);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    static <T> Constructor<T> getConstructor(Class<T> sourceClass, Class<?>... parameterTypes) {
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<Constructor<T>>() {
            @Override
            public Constructor<T> run() {
                try {
                    Constructor<T> constructor = sourceClass.getDeclaredConstructor(parameterTypes);
                    constructor.setAccessible(true);
                    return constructor;
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    static <T> T newInstance(Constructor<T> constructor, Object... parameters) {
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<T>() {
            @Override
            public T run() {
                try {
                    return constructor.newInstance(parameters);
                } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    static Object getValue(Object source, Field field) {
        return getValue(source, field, Object.class);
    }

    static <T> T getValue(Object source, Field field, Class<T> fieldType) {
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<T>() {
            @Override
            public T run() {
                try {
                    return fieldType.cast(field.get(source));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    static void setValue(Object source, Field field, Object value) {
        WildFlySecurityManager.doUnchecked(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                try {
                    field.set(source, value);
                    return null;
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    static Object invoke(Object source, Method method) {
        return invoke(source, method, Object.class);
    }

    static <T> T invoke(Object source, Method method, Class<T> returnClass) {
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<T>() {
            @Override
            public T run() {
                try {
                    return returnClass.cast(method.invoke(source));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    static Object invoke(Object source, String methodName) {
        return invoke(source, methodName, Object.class);
    }

    static <T> T invoke(Object source, String methodName, Class<T> returnType) {
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<T>() {
            @Override
            public T run() {
                Method method = findMethod(source.getClass(), methodName);
                return invoke(source, method, returnType);
            }
        });
    }
}
