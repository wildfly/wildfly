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

import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Default bean info.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@SuppressWarnings({"unchecked"})
public class DefaultBeanInfo<T> implements BeanInfo<T> {
    private final List<ClassReflectionIndex> indexes = new ArrayList<ClassReflectionIndex>();
    private final Class beanClass;
    private DeploymentReflectionIndex index;
    private Class currentClass;

    public DefaultBeanInfo(DeploymentReflectionIndex index, Class<T> beanClass) {
        this.index = index;
        this.beanClass = beanClass;
        this.currentClass = beanClass;
    }

    /**
     * Do lazy lookup.
     *
     * @param lookup the lookup
     * @param start the start
     * @param depth the depth
     * @return reflection index result
     */
    protected <U> U lookup(Lookup<U> lookup, int start, int depth) {
        int size;
        synchronized (indexes) {
            size = indexes.size();
            for (int i = start; i < depth && i < size; i++) {
                U result = lookup.lookup(indexes.get(i));
                if (result != null)
                    return result;
            }
        }

        if (currentClass == null)
            return null;

        synchronized (indexes) {
            ClassReflectionIndex cri = index.getClassIndex(currentClass);
            indexes.add(cri);
            currentClass = currentClass.getSuperclass();
        }
        return lookup(lookup, size, depth);
    }

    public Constructor<T> getConstructor(final String... parameterTypes) {
        return lookup(
                new Lookup<Constructor<T>>() {
                    public Constructor<T> lookup(ClassReflectionIndex index) {
                        final Constructor ctor = index.getConstructor(parameterTypes);
                        if (ctor == null)
                            throw PojoLogger.ROOT_LOGGER.ctorNotFound(Arrays.toString(parameterTypes), beanClass.getName());
                        return ctor;
                    }
                }, 0, 1);
    }

    @Override
    public Constructor<T> findConstructor(final String... parameterTypes) {
        return lookup(new Lookup<Constructor<T>>() {
            @Override
            public Constructor<T> lookup(ClassReflectionIndex index) {
                final Collection<Constructor<?>> ctors = index.getConstructors();
                for (Constructor c : ctors) {
                    if (Configurator.equals(parameterTypes, c.getParameterTypes()))
                        return c;
                }
                throw PojoLogger.ROOT_LOGGER.ctorNotFound(Arrays.toString(parameterTypes), beanClass.getName());
            }
        }, 0, 1);
    }

    @Override
    public Field getField(final String name) {
        final Field lookup = lookup(new Lookup<Field>() {
            @Override
            public Field lookup(ClassReflectionIndex index) {
                return index.getField(name);
            }
        }, 0, Integer.MAX_VALUE);
        if (lookup == null)
            throw PojoLogger.ROOT_LOGGER.fieldNotFound(name, beanClass.getName());
        return lookup;
    }

    @Override
    public Method getMethod(final String name, final String... parameterTypes) {
        final Method lookup = lookup(new Lookup<Method>() {
            @Override
            public Method lookup(ClassReflectionIndex index) {
                Collection<Method> methods = index.getMethods(name, parameterTypes);
                int size = methods.size();
                switch (size) {
                    case 0:
                        return null;
                    case 1:
                        return methods.iterator().next();
                    default:
                        throw PojoLogger.ROOT_LOGGER.ambiguousMatch(methods, name, beanClass.getName());
                }
            }
        }, 0, Integer.MAX_VALUE);
        if (lookup == null)
            throw PojoLogger.ROOT_LOGGER.methodNotFound(name, Arrays.toString(parameterTypes), beanClass.getName());
        return lookup;
    }

    @Override
    public Method findMethod(String name, String... parameterTypes) {
        return Configurator.findMethod(index, beanClass, name, parameterTypes, false, true, true);
    }

    @Override
    public Method getGetter(final String propertyName, final Class<?> type) {
        final boolean isBoolean = Boolean.TYPE.equals(type);
        final String name = ((isBoolean) ? "is" : "get") + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        final Method result = lookup(new Lookup<Method>() {
            @Override
            public Method lookup(ClassReflectionIndex index) {
                Collection<Method> methods = index.getAllMethods(name, 0);
                if (type == null) {
                    if (methods.size() == 1)
                        return methods.iterator().next();
                    else
                        return null;
                }
                for (Method m : methods) {
                    Class<?> pt = m.getReturnType();
                    if (pt.isAssignableFrom(type))
                        return m;
                }
                return null;
            }
        }, 0, Integer.MAX_VALUE);
        if (result == null)
            throw PojoLogger.ROOT_LOGGER.getterNotFound(type, beanClass.getName());
        return result;
    }

    @Override
    public Method getSetter(final String propertyName, final Class<?> type) {
        final String name = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        final Method result = lookup(new Lookup<Method>() {
            @Override
            public Method lookup(ClassReflectionIndex index) {
                Collection<Method> methods = index.getAllMethods(name, 1);
                if (type == null) {
                    if (methods.size() == 1)
                        return methods.iterator().next();
                    else
                        return null;
                }
                for (Method m : methods) {
                    Class<?> pt = m.getParameterTypes()[0];
                    if (pt.isAssignableFrom(type))
                        return m;
                }
                return null;
            }
        }, 0, Integer.MAX_VALUE);
        if (result == null)
            throw PojoLogger.ROOT_LOGGER.setterNotFound(type, beanClass.getName());
        return result;
    }

    private interface Lookup<U> {
        U lookup(ClassReflectionIndex index);
    }
}
