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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A short-lived index of all the declared fields and methods of a class.
 *
 * The ClassReflectionIndex is only available during the deployment.
 *
 * @param <T> the type being indexed
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ClassReflectionIndex<T> {
    private final Class<T> indexedClass;
    private final Map<String, Field> fields;
    private final Map<ParamList, Constructor<T>> constructors;
    private final Map<String, Map<ParamList, Map<Class<?>, Method>>> methods;

    @SuppressWarnings( { "unchecked" })
    ClassReflectionIndex(final Class<T> indexedClass) {
        this.indexedClass = indexedClass;
        // -- fields --
        final Field[] declaredFields = indexedClass.getDeclaredFields();
        final Map<String, Field> fields = new HashMap<String, Field>();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            fields.put(field.getName(), field);
        }
        this.fields = fields;
        // -- methods --
        final Method[] declaredMethods = indexedClass.getDeclaredMethods();
        final Map<String, Map<ParamList, Map<Class<?>, Method>>> methods = new HashMap<String, Map<ParamList, Map<Class<?>, Method>>>();
        for (Method method : declaredMethods) {
            method.setAccessible(true);
            addMethod(methods, method);
        }
        // add all public methods as well
        for (Method method : indexedClass.getMethods()) {
            addMethod(methods, method);
        }
        this.methods = methods;
        // -- constructors --
        final Constructor<T>[] declaredConstructors = (Constructor<T>[]) indexedClass.getDeclaredConstructors();
        final Map<ParamList, Constructor<T>> constructors = new HashMap<ParamList, Constructor<T>>();
        for (Constructor<T> constructor : declaredConstructors) {
            constructor.setAccessible(true);
            constructors.put(createParamList(constructor.getParameterTypes()), constructor);
        }
        this.constructors = constructors;
    }

    private static final ParamList EMPTY = new ParamList(new Class<?>[0]);

    private static void addMethod(Map<String, Map<ParamList, Map<Class<?>, Method>>> methods, Method method) {
        final String name = method.getName();
        Map<ParamList, Map<Class<?>, Method>> nameMap = methods.get(name);
        if (nameMap == null) {
            methods.put(name, nameMap = new HashMap<ParamList, Map<Class<?>, Method>>());
        }
        final Class<?>[] types = method.getParameterTypes();
        final ParamList list = createParamList(types);
        Map<Class<?>, Method> paramsMap = nameMap.get(list);
        if (paramsMap == null) {
            nameMap.put(list, paramsMap = new HashMap<Class<?>, Method>());
        }
        paramsMap.put(method.getReturnType(), method);
    }

    private static ParamList createParamList(final Class<?>[] types) {
        return types == null || types.length == 0 ? EMPTY : new ParamList(types);
    }

    /**
     * Get the class indexed by this object.
     *
     * @return the class
     */
    public Class<T> getIndexedClass() {
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
     * Get a collection of fields declared on this object.
     *
     * @return The (possibly empty) collection of all declared fields on this object
     */
    public Collection<Field> getFields() {
        return Collections.unmodifiableCollection(fields.values());
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
        final Map<Class<?>, Method> paramsMap = nameMap.get(createParamList(paramTypes));
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
        final Map<Class<?>, Method> paramsMap = nameMap.get(createParamList(paramTypes));
        if (paramsMap == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableCollection(paramsMap.values());
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

    /**
     * Get a collection of methods declared on this object.
     *
     * @return the (possibly empty) collection of all declared methods
     */
    public Collection<Method> getMethods() {
        final Collection<Method> methods = new ArrayList<Method>();
        for (Map.Entry<String, Map<ParamList, Map<Class<?>, Method>>> entry : this.methods.entrySet()) {
            final Map<ParamList, Map<Class<?>, Method>> nameMap = entry.getValue();
            for (Map<Class<?>, Method> map : nameMap.values()) {
                methods.addAll(map.values());
            }
        }
        return methods;
    }

    /**
     * Get the full collection of constructors declared on this object.
     *
     * @return the constructors
     */
    public Collection<Constructor<T>> getConstructors() {
        return Collections.unmodifiableCollection(constructors.values());
    }

    /**
     * Get a constructor declared on this class.
     *
     * @param paramTypes the constructor argument types
     * @return the constructor, or {@code null} of no such constructor exists
     */
    public Constructor<T> getConstructor(Class<?>... paramTypes) {
        return constructors.get(createParamList(paramTypes));
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
