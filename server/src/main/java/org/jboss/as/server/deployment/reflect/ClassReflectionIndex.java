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

package org.jboss.as.server.deployment.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An index of all the declared fields and methods of a class.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ClassReflectionIndex {
    private final Class<?> indexedClass;
    private final Map<String, Field> fields;
    private final Map<String, Map<ParamList, Map<Class<?>, Method>>> methods;

    ClassReflectionIndex(final Class<?> indexedClass) {
        this.indexedClass = indexedClass;
        final Field[] declaredFields = indexedClass.getDeclaredFields();
        final Map<String, Field> fields = new HashMap<String, Field>();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            fields.put(field.getName(), field);
        }
        this.fields = fields;
        final Method[] declaredMethods = indexedClass.getDeclaredMethods();
        final Map<String, Map<ParamList, Map<Class<?>, Method>>> methods = new HashMap<String, Map<ParamList, Map<Class<?>, Method>>>();
        for (Method method : declaredMethods) {
            method.setAccessible(true);
            final String name = method.getName();
            Map<ParamList, Map<Class<?>, Method>> nameMap = methods.get(name);
            if (nameMap == null) {
                methods.put(name, nameMap = new HashMap<ParamList, Map<Class<?>, Method>>());
            }
            final Class<?>[] types = method.getParameterTypes();
            final ParamList list = types.length == 0 ? EMPTY : new ParamList(types);
            Map<Class<?>, Method> paramsMap = nameMap.get(list);
            if (paramsMap == null) {
                nameMap.put(list, paramsMap = new HashMap<Class<?>, Method>());
            }
            paramsMap.put(method.getReturnType(), method);
        }
        this.methods = methods;
    }

    private static final ParamList EMPTY = new ParamList(new Class<?>[0]);

    /**
     * Get the class indexed by this object.
     *
     * @return the class
     */
    public Class<?> getIndexedClass() {
        return indexedClass;
    }

    /**
     * Get a field declared on this object.
     *
     * @param name the field name
     * @return the field, or {@code null} if no field of that name exists
     */
    public Field getField(String name) {
        return fields.get(name);
    }

    /**
     * Get a method declared on this object.
     *
     * @param returnType the method return type
     * @param name the name of the method
     * @param paramTypes the parameter types of the method
     * @return the method, or {@code null} if no method of that description exists
     */
    public Method getMethod(Class<?> returnType, String name, Class<?>... paramTypes) {
        final Map<ParamList, Map<Class<?>, Method>> nameMap = methods.get(name);
        if (nameMap == null) {
            return null;
        }
        final Map<Class<?>, Method> paramsMap = nameMap.get(new ParamList(paramTypes));
        if (paramsMap == null) {
            return null;
        }
        return paramsMap.get(returnType);
    }

    /**
     * Get a collection of methods declared on this object.
     *
     * @param name the name of the method
     * @param paramTypes the parameter types of the method
     * @return the (possibly empty) collection of methods matching the description
     */
    public Collection<Method> getMethods(String name, Class<?>... paramTypes) {
        final Map<ParamList, Map<Class<?>, Method>> nameMap = methods.get(name);
        if (nameMap == null) {
            return Collections.emptySet();
        }
        final Map<Class<?>, Method> paramsMap = nameMap.get(new ParamList(paramTypes));
        if (paramsMap == null) {
            return Collections.emptySet();
        }
        return paramsMap.values();
    }

    /**
     * Get a collection of methods declared on this object.
     *
     * @param name the name of the method
     * @return the (possibly empty) collection of methods with the given name
     */
    public Collection<Method> getMethods(String name) {
        final Map<ParamList, Map<Class<?>, Method>> nameMap = methods.get(name);
        if (nameMap == null) {
            return Collections.emptySet();
        }
        final Collection<Method> methods = new ArrayList<Method>();
        for (Map<Class<?>, Method> map : nameMap.values()) {
            methods.addAll(map.values());
        }
        return methods;
    }

    private static final class ParamList {
        private final Class<?>[] types;
        private final int hashCode;

        ParamList(final Class<?>[] types) {
            this.types = types;
            hashCode = Arrays.hashCode(types);
        }

        Class<?>[] getTypes() {
            return types;
        }

        public boolean equals(Object other) {
            return other instanceof ParamList && equals((ParamList)other);
        }

        public boolean equals(ParamList other) {
            return this == other || other != null && Arrays.equals(types, other.types);
        }

        public int hashCode() {
            return hashCode;
        }
    }
}
