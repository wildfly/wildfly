/*
 * The Fungal kernel project
 * Copyright (C) 2010
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.connector.util;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Injection utility which can inject values into objects
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class Injection {
    /**
     * Constructor
     */
    public Injection() {
    }

    /**
     * Inject a value into an object property
     * @param propertyType The property type as a fully quilified class name
     * @param propertyName The property name
     * @param propertyValue The property value
     * @param object The object
     * @exception NoSuchMethodException If the property method cannot be found
     * @exception IllegalAccessException If the property method cannot be
     *            accessed
     * @exception InvocationTargetException If the property method cannot be
     *            executed
     */
    public void inject(String propertyType, String propertyName, String propertyValue, Object object)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (propertyName == null || propertyName.trim().equals(""))
            throw new IllegalArgumentException("PropertyName is undefined");

        if (object == null)
            throw new IllegalArgumentException("Object is null");

        String methodName = "set" + propertyName.substring(0, 1).toUpperCase(Locale.US);
        if (propertyName.length() > 1) {
            methodName += propertyName.substring(1);
        }

        if (propertyType == null || propertyType.trim().equals("")) {
            Method[] methods = object.getClass().getMethods();
            if (methods != null) {
                for (int i = 0; propertyType == null && i < methods.length; i++) {
                    Method method = methods[i];
                    if (methodName.equals(method.getName()) && method.getParameterTypes().length == 1) {
                        propertyType = method.getParameterTypes()[0].getName();
                    }
                }
            }
        }

        if (propertyType == null || propertyType.trim().equals(""))
            throw new IllegalArgumentException("PropertyType is undefined");

        Class parameterClass = null;
        Object parameterValue = null;

        String substituredValue = getSubstitutionValue(propertyValue);

        if (propertyType.equals("java.lang.String")) {
            parameterClass = String.class;
            parameterValue = substituredValue;
        } else if (propertyType.equals("byte") || propertyType.equals("java.lang.Byte")) {
            parameterClass = Byte.class;
            if (substituredValue != null && !substituredValue.trim().equals(""))
                parameterValue = Byte.valueOf(substituredValue);
        } else if (propertyType.equals("short") || propertyType.equals("java.lang.Short")) {
            parameterClass = Short.class;
            if (substituredValue != null && !substituredValue.trim().equals(""))
                parameterValue = Short.valueOf(substituredValue);
        } else if (propertyType.equals("int") || propertyType.equals("java.lang.Integer")) {
            parameterClass = Integer.class;
            if (substituredValue != null && !substituredValue.trim().equals(""))
                parameterValue = Integer.valueOf(substituredValue);
        } else if (propertyType.equals("long") || propertyType.equals("java.lang.Long")) {
            parameterClass = Long.class;
            if (substituredValue != null && !substituredValue.trim().equals(""))
                parameterValue = Long.valueOf(substituredValue);
        } else if (propertyType.equals("float") || propertyType.equals("java.lang.Float")) {
            parameterClass = Float.class;
            if (substituredValue != null && !substituredValue.trim().equals(""))
                parameterValue = Float.valueOf(substituredValue);
        } else if (propertyType.equals("double") || propertyType.equals("java.lang.Double")) {
            parameterClass = Double.class;
            if (substituredValue != null && !substituredValue.trim().equals(""))
                parameterValue = Double.valueOf(substituredValue);
        } else if (propertyType.equals("boolean") || propertyType.equals("java.lang.Boolean")) {
            parameterClass = Boolean.class;
            if (substituredValue != null && !substituredValue.trim().equals(""))
                parameterValue = Boolean.valueOf(substituredValue);
        } else if (propertyType.equals("char") || propertyType.equals("java.lang.Character")) {
            parameterClass = Character.class;
            if (substituredValue != null && !substituredValue.trim().equals(""))
                parameterValue = Character.valueOf(substituredValue.charAt(0));
        } else {
            throw new IllegalArgumentException("Unknown property type: " + propertyType + " for " + "property " + propertyName);
        }

        Method method = null;
        boolean objectInjection = true;

        try {
            method = object.getClass().getMethod(methodName, parameterClass);
        } catch (NoSuchMethodException nsme) {
            objectInjection = false;

            if (parameterClass.equals(Byte.class)) {
                parameterClass = byte.class;
            } else if (parameterClass.equals(Short.class)) {
                parameterClass = short.class;
            } else if (parameterClass.equals(Integer.class)) {
                parameterClass = int.class;
            } else if (parameterClass.equals(Long.class)) {
                parameterClass = long.class;
            } else if (parameterClass.equals(Float.class)) {
                parameterClass = float.class;
            } else if (parameterClass.equals(Double.class)) {
                parameterClass = double.class;
            } else if (parameterClass.equals(Boolean.class)) {
                parameterClass = boolean.class;
            } else if (parameterClass.equals(Character.class)) {
                parameterClass = char.class;
            }

            method = object.getClass().getMethod(methodName, parameterClass);
        }

        if (objectInjection || parameterValue != null)
            method.invoke(object, new Object[] { parameterValue });
    }

    /**
     * System property substitution
     * @param input The input string
     * @return The output
     */
    private String getSubstitutionValue(String input) {
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
                    systemProperty = SecurityActions.getSystemProperty(s);
                }
            } else {
                systemProperty = SecurityActions.getSystemProperty(input.substring(from + 2, dv));
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
            } else if (defaultValue != null && !defaultValue.trim().equals("")) {
                input = prefix + defaultValue + postfix;
            }
        }
        return input;
    }
}
