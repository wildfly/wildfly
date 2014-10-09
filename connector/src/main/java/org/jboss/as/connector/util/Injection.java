/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.util;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Injection utility which can inject values into objects. This file is a copy
 * of the <code>com.github.fungal.api.util.Injection</code> class.
 *
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class Injection {
    /**
     * Constructor
     */
    public Injection() {
    }

    /**
     * Inject a value into an object property
     *
     * @param object        The object
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @throws NoSuchMethodException     If the property method cannot be found
     * @throws IllegalAccessException    If the property method cannot be accessed
     * @throws InvocationTargetException If the property method cannot be executed
     */
    @SuppressWarnings("unchecked")
    public void inject(Object object, String propertyName, Object propertyValue)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        inject(object, propertyName, propertyValue, null, false);
    }

    /**
     * Inject a value into an object property
     *
     * @param object        The object
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @param propertyType  The property type as a fully quilified class name
     * @throws NoSuchMethodException     If the property method cannot be found
     * @throws IllegalAccessException    If the property method cannot be accessed
     * @throws InvocationTargetException If the property method cannot be executed
     */
    @SuppressWarnings("unchecked")
    public void inject(Object object, String propertyName, Object propertyValue, String propertyType)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        inject(object, propertyName, propertyValue, propertyType, false);
    }

    /**
     * Inject a value into an object property
     *
     * @param object        The object
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @param propertyType  The property type as a fully quilified class name
     * @param includeFields Should fields be included for injection if a method can't be found
     * @throws NoSuchMethodException     If the property method cannot be found
     * @throws IllegalAccessException    If the property method cannot be accessed
     * @throws InvocationTargetException If the property method cannot be executed
     */
    @SuppressWarnings("unchecked")
    public void inject(Object object,
                       String propertyName, Object propertyValue, String propertyType,
                       boolean includeFields)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (object == null)
            throw new IllegalArgumentException(ConnectorLogger.ROOT_LOGGER.nullVar("Object"));

        if (propertyName == null || propertyName.trim().equals(""))
            throw ConnectorLogger.ROOT_LOGGER.undefinedVar("PropertyName");

        String methodName = "set" + propertyName.substring(0, 1).toUpperCase(Locale.US);
        if (propertyName.length() > 1) {
            methodName += propertyName.substring(1);
        }

        Method method = findMethod(object.getClass(), methodName, propertyType);

        if (method != null) {
            Class<?> parameterClass = method.getParameterTypes()[0];
            Object parameterValue = null;
            try {
                parameterValue = getValue(propertyName, parameterClass, propertyValue,
                        WildFlySecurityManager.getClassLoaderPrivileged(object.getClass()));
            } catch (Throwable t) {
                throw new InvocationTargetException(t, t.getMessage());
            }

            if (!parameterClass.isPrimitive() || parameterValue != null)
                method.invoke(object, new Object[]{parameterValue});
        } else {
            if (!includeFields)
                throw ConnectorLogger.ROOT_LOGGER.noSuchMethod(methodName);

            // Ok, we didn't find a method - assume field
            Field field = findField(object.getClass(), propertyName, propertyType);

            if (field != null) {
                Class<?> fieldClass = field.getType();
                Object fieldValue = null;
                try {
                    fieldValue = getValue(propertyName, fieldClass, propertyValue,
                            WildFlySecurityManager.getClassLoaderPrivileged(object.getClass()));
                } catch (Throwable t) {
                    throw new InvocationTargetException(t, t.getMessage());
                }

                field.set(object, fieldValue);
            } else {
                throw ConnectorLogger.ROOT_LOGGER.noSuchField(propertyName);
            }
        }
    }

    /**
     * Compare the type of a class with the actual value
     *
     * @param classType    The class type
     * @param propertyType The property type
     * @return True if they match, or if there is a primitive mapping
     */
    private boolean argumentMatches(String classType, String propertyType) {
        return (classType.equals(propertyType))
                || (classType.equals("java.lang.Byte") && propertyType.equals("byte"))
                || (classType.equals("java.lang.Short") && propertyType.equals("short"))
                || (classType.equals("java.lang.Integer") && propertyType.equals("int"))
                || (classType.equals("java.lang.Long") && propertyType.equals("long"))
                || (classType.equals("java.lang.Float") && propertyType.equals("float"))
                || (classType.equals("java.lang.Double") && propertyType.equals("double"))
                || (classType.equals("java.lang.Boolean") && propertyType.equals("boolean"))
                || (classType.equals("java.lang.Character") && propertyType.equals("char"));
    }

    /**
     * Find a method
     *
     * @param clz          The class
     * @param methodName   The method name
     * @param propertyType The property type; can be <code>null</code>
     * @return The method; <code>null</code> if not found
     */
    protected Method findMethod(Class<?> clz, String methodName, String propertyType) {
        while (!clz.equals(Object.class)) {
            List<Method> hits = null;
            Method[] methods = SecurityActions.getDeclaredMethods(clz);
            for (int i = 0; i < methods.length; i++) {
                final Method method = methods[i];
                if (methodName.equals(method.getName()) && method.getParameterTypes().length == 1) {
                    if (propertyType == null || argumentMatches(propertyType, method.getParameterTypes()[0].getName())) {
                        if (hits == null)
                            hits = new ArrayList<Method>(1);

                        SecurityActions.setAccessible(method);

                        hits.add(method);
                    }
                }
            }

            if (hits != null) {
                if (hits.size() == 1) {
                    return hits.get(0);
                } else {
                    Collections.sort(hits, new MethodSorter());
                    if (propertyType != null) {
                        for (Method m : hits) {
                            if (propertyType.equals(m.getParameterTypes()[0].getName()))
                                return m;
                        }
                    }

                    return hits.get(0);
                }
            }

            clz = clz.getSuperclass();
        }

        return null;
    }

    /**
     * Find a field
     *
     * @param clz       The class
     * @param fieldName The field name
     * @param fieldType The field type; can be <code>null</code>
     * @return The field; <code>null</code> if not found
     */
    protected Field findField(Class<?> clz, String fieldName, String fieldType) {
        while (!clz.equals(Object.class)) {
            List<Field> hits = null;
            Field[] fields = SecurityActions.getDeclaredFields(clz);
            for (int i = 0; i < fields.length; i++) {
                final Field field = fields[i];
                if (fieldName.equals(field.getName())) {
                    if (fieldType == null || argumentMatches(fieldType, field.getType().getName())) {
                        if (hits == null)
                            hits = new ArrayList<Field>(1);

                        SecurityActions.setAccessible(field);

                        hits.add(field);
                    }
                }
            }

            if (hits != null) {
                if (hits.size() == 1) {
                    return hits.get(0);
                } else {
                    Collections.sort(hits, new FieldSorter());
                    if (fieldType != null) {
                        for (Field f : hits) {
                            if (fieldType.equals(f.getType().getName()))
                                return f;
                        }
                    }

                    return hits.get(0);
                }
            }

            clz = clz.getSuperclass();
        }

        return null;
    }

    /**
     * Get the value
     *
     * @param name The value name
     * @param clz  The value class
     * @param v    The value
     * @param cl   The class loader
     * @return The substituted value
     * @throws Exception Thrown in case of an error
     */
    protected Object getValue(String name, Class<?> clz, Object v, ClassLoader cl) throws Exception {
        if (v instanceof String) {
            String substituredValue = getSubstitutionValue((String) v);

            if (clz.equals(String.class)) {
                v = substituredValue;
            } else if (clz.equals(byte.class) || clz.equals(Byte.class)) {
                if (substituredValue != null && !substituredValue.trim().equals(""))
                    v = Byte.valueOf(substituredValue);
            } else if (clz.equals(short.class) || clz.equals(Short.class)) {
                if (substituredValue != null && !substituredValue.trim().equals(""))
                    v = Short.valueOf(substituredValue);
            } else if (clz.equals(int.class) || clz.equals(Integer.class)) {
                if (substituredValue != null && !substituredValue.trim().equals(""))
                    v = Integer.valueOf(substituredValue);
            } else if (clz.equals(long.class) || clz.equals(Long.class)) {
                if (substituredValue != null && !substituredValue.trim().equals(""))
                    v = Long.valueOf(substituredValue);
            } else if (clz.equals(float.class) || clz.equals(Float.class)) {
                if (substituredValue != null && !substituredValue.trim().equals(""))
                    v = Float.valueOf(substituredValue);
            } else if (clz.equals(double.class) || clz.equals(Double.class)) {
                if (substituredValue != null && !substituredValue.trim().equals(""))
                    v = Double.valueOf(substituredValue);
            } else if (clz.equals(boolean.class) || clz.equals(Boolean.class)) {
                if (substituredValue != null && !substituredValue.trim().equals(""))
                    v = Boolean.valueOf(substituredValue);
            } else if (clz.equals(char.class) || clz.equals(Character.class)) {
                if (substituredValue != null && !substituredValue.trim().equals(""))
                    v = Character.valueOf(substituredValue.charAt(0));
            } else if (clz.equals(InetAddress.class)) {
                v = InetAddress.getByName(substituredValue);
            } else if (clz.equals(Class.class)) {
                v = Class.forName(substituredValue, true, cl);
            } else if (clz.equals(Properties.class)) {
                Properties prop = new Properties();

                StringTokenizer st = new StringTokenizer(substituredValue, " ,");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    String key = "";
                    String value = "";

                    int index = token.indexOf("=");
                    if (index != -1) {
                        key = token.substring(0, index);

                        if (token.length() > index + 1)
                            value = token.substring(index + 1);
                    } else {
                        key = token;
                    }

                    if (!"".equals(key))
                        prop.setProperty(key, value);
                }

                v = prop;
            } else {
                try {
                    Constructor<?> constructor = SecurityActions.getConstructor(clz, String.class);
                    v = constructor.newInstance(substituredValue);
                } catch (Throwable t) {
                    // Try static String valueOf method
                    try {
                        Method valueOf = SecurityActions.getMethod(clz, "valueOf", String.class);
                        v = valueOf.invoke((Object) null, substituredValue);
                    } catch (Throwable inner) {
                        throw ConnectorLogger.ROOT_LOGGER.noPropertyResolution(name);
                    }
                }
            }
        }

        return v;
    }

    /**
     * System property substitution
     *
     * @param input The input string
     * @return The output
     */
    protected String getSubstitutionValue(String input) {
        if (input == null || input.trim().equals(""))
            return input;

        while (input.indexOf("${") != -1) {
            int from = input.indexOf("${");
            int to = input.indexOf("}");
            int dv = input.indexOf(":", from + 2);

            if (dv != -1) {
                if (dv > to)
                    dv = -1;
            }

            String systemProperty = "";
            String defaultValue = "";
            if (dv == -1) {
                String s = input.substring(from + 2, to);
                if ("/".equals(s)) {
                    systemProperty = File.separator;
                } else if (":".equals(s)) {
                    systemProperty = File.pathSeparator;
                } else {
                    systemProperty = WildFlySecurityManager.getSystemPropertiesPrivileged().getProperty(s);
                }
            } else {
                systemProperty = WildFlySecurityManager.getSystemPropertiesPrivileged().getProperty(input.substring(from + 2, dv));
                defaultValue = input.substring(dv + 1, to);
            }
            String prefix = "";
            String postfix = "";

            if (from != 0) {
                prefix = input.substring(0, from);
            }

            if (to + 1 < input.length() - 1) {
                postfix = input.substring(to + 1);
            }

            if (systemProperty != null && !systemProperty.trim().equals("")) {
                input = prefix + systemProperty + postfix;
            } else if (!defaultValue.trim().equals("")) {
                input = prefix + defaultValue + postfix;
            } else {
                input = prefix + postfix;
            }
        }
        return input;
    }

    /**
     * Method sorter
     */
    static class MethodSorter implements Comparator<Method>, Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor
         */
        MethodSorter() {
        }

        /**
         * {@inheritDoc}
         */
        public int compare(Method o1, Method o2) {
            int m1 = o1.getModifiers();
            int m2 = o2.getModifiers();

            if (Modifier.isPublic(m1))
                return -1;

            if (Modifier.isPublic(m2))
                return 1;

            if (Modifier.isProtected(m1))
                return -1;

            if (Modifier.isProtected(m2))
                return 1;

            if (Modifier.isPrivate(m1))
                return -1;

            if (Modifier.isPrivate(m2))
                return 1;

            return 0;
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null || !(o instanceof MethodSorter))
                return false;

            return true;
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return 42;
        }
    }

    /**
     * Field sorter
     */
    static class FieldSorter implements Comparator<Field>, Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor
         */
        FieldSorter() {
        }

        /**
         * {@inheritDoc}
         */
        public int compare(Field o1, Field o2) {
            int m1 = o1.getModifiers();
            int m2 = o2.getModifiers();

            if (Modifier.isPublic(m1))
                return -1;

            if (Modifier.isPublic(m2))
                return 1;

            if (Modifier.isProtected(m1))
                return -1;

            if (Modifier.isProtected(m2))
                return 1;

            if (Modifier.isPrivate(m1))
                return -1;

            if (Modifier.isPrivate(m2))
                return 1;

            return 0;
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null || !(o instanceof FieldSorter))
                return false;

            return true;
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return 42;
        }
    }
}

