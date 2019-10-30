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
import org.jboss.as.pojo.descriptor.ValueConfig;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.common.beans.property.PropertiesValueResolver;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;

/**
 * Configuration util.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class Configurator {

    /**
     * No parameter types
     */
    public static final String[] NO_PARAMS_TYPES = new String[0];

    /**
     * Turn type into class.
     *
     * @param type the type
     * @return class
     */
    public static Class<?> toClass(Type type) {
        if (type instanceof Class) {
            return (Class) type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            return toClass(pt.getRawType());
        } else {
            throw PojoLogger.ROOT_LOGGER.unknownType(type);
        }
    }

    /**
     * Convert a value
     *
     * @param clazz             the class
     * @param value             the value
     * @param replaceProperties whether to replace system properties
     * @param trim              whether to trim string value
     * @return the value or null if there is no editor
     * @throws Throwable for any error
     */
    @SuppressWarnings("unchecked")
    public static Object convertValue(Class<?> clazz, Object value, boolean replaceProperties, boolean trim) throws Throwable {
        if (clazz == null)
            return value;
        if (value == null)
            return null;

        Class<?> valueClass = value.getClass();

        // If we have a string, trim and replace any system properties when requested
        if (valueClass == String.class) {
            String string = (String) value;
            if (trim)
                string = string.trim();
            if (replaceProperties)
                value = PropertiesValueResolver.replaceProperties(string);
        }

        if (clazz.isAssignableFrom(valueClass))
            return value;

        // First see if this is an Enum
        if (clazz.isEnum()) {
            Class<? extends Enum> eclazz = clazz.asSubclass(Enum.class);
            return Enum.valueOf(eclazz, value.toString());
        }

        // Next look for a property editor
        if (valueClass == String.class) {
            PropertyEditor editor = PropertyEditorManager.findEditor(clazz);
            if (editor != null) {
                editor.setAsText((String) value);
                return editor.getValue();
            }
        }

        // Try a static clazz.valueOf(value)
        try {
            Method method = clazz.getMethod("valueOf", valueClass);
            int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)
                    && clazz.isAssignableFrom(method.getReturnType()))
                return method.invoke(null, value);
        } catch (Exception ignored) {
        }

        if (valueClass == String.class) {
            try {
                Constructor constructor = clazz.getConstructor(valueClass);
                if (Modifier.isPublic(constructor.getModifiers()))
                    return constructor.newInstance(value);
            } catch (Exception ignored) {
            }
        }

        return value;
    }

    /**
     * Get types from values.
     *
     * @param values the values
     * @return the values' types
     */
    public static String[] getTypes(ValueConfig[] values) {
        if (values == null || values.length == 0)
            return NO_PARAMS_TYPES;

        String[] types = new String[values.length];
        for (int i =0; i < types.length; i++)
            types[i] = values[i].getType();
        return types;
    }

    /**
     * Find method info
     *
     * @param index      the deployment reflection index
     * @param classInfo  the class info
     * @param name       the method name
     * @param paramTypes the parameter types
     * @param isStatic   must the method be static
     * @param isPublic   must the method be public
     * @param strict     is strict about method modifiers
     * @return the method info
     * @throws IllegalArgumentException when no such method
     */
    @SuppressWarnings("unchecked")
    public static Method findMethod(DeploymentReflectionIndex index, Class classInfo, String name, String[] paramTypes, boolean isStatic, boolean isPublic, boolean strict) throws IllegalArgumentException {
        if (name == null)
            throw PojoLogger.ROOT_LOGGER.nullName();

        if (classInfo == null)
            throw PojoLogger.ROOT_LOGGER.nullClassInfo();

        if (paramTypes == null)
            paramTypes = NO_PARAMS_TYPES;

        Class current = classInfo;
        while (current != null) {
            ClassReflectionIndex cri = index.getClassIndex(classInfo);
            Method result = locateMethod(cri, name, paramTypes, isStatic, isPublic, strict);
            if (result != null)
                return result;
            current = current.getSuperclass();
        }
        throw PojoLogger.ROOT_LOGGER.methodNotFound(name, Arrays.toString(paramTypes), classInfo.getName());
    }

    /**
     * Find method info
     *
     * @param classInfo  the class info
     * @param name       the method name
     * @param paramTypes the parameter types
     * @param isStatic   must the method be static
     * @param isPublic   must the method be public
     * @param strict     is strict about method modifiers
     * @return the method info or null if not found
     */
    @SuppressWarnings("unchecked")
    private static Method locateMethod(ClassReflectionIndex classInfo, String name, String[] paramTypes, boolean isStatic, boolean isPublic, boolean strict) {
        Collection<Method> methods = classInfo.getMethods();
        if (methods != null) {
            for (Method method : methods) {
                if (name.equals(method.getName()) &&
                        equals(paramTypes, method.getParameterTypes()) &&
                        (strict == false || (Modifier.isStatic(method.getModifiers()) == isStatic && Modifier.isPublic(method.getModifiers()) == isPublic)))
                    return method;
            }
        }
        return null;
    }

    /**
     * Test whether type names are equal to type infos
     *
     * @param typeNames the type names
     * @param typeInfos the type infos
     * @return true when they are equal
     */
    public static boolean equals(String[] typeNames, Class<?>[] typeInfos) {
        if (simpleCheck(typeNames, typeInfos) == false)
            return false;

        for (int i = 0; i < typeNames.length; ++i) {
            if (typeNames[i] != null && typeNames[i].equals(typeInfos[i].getName()) == false)
                return false;
        }
        return true;
    }

    /**
     * A simple null and length check.
     *
     * @param typeNames the type names
     * @param typeInfos the type infos
     * @return false if either argument is null or lengths differ, else true
     */
    protected static boolean simpleCheck(String[] typeNames, Class<?>[] typeInfos) {
        return typeNames != null && typeInfos != null && typeNames.length == typeInfos.length;
    }
}
