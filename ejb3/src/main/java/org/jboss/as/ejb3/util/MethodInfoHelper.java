/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.util;

import java.lang.reflect.Method;

/**
 * This helper class contains helper methods that are used
 * to resolve method-params in deployment descriptors and method level annotations
 * in Jakarta Enterprise Beans implementation classes.
 *
 * @author robert.panzer@me.com
 *
 */
public final class MethodInfoHelper {
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    private MethodInfoHelper() {}

    /**
     * This method returns the class names of the parameters of the given method
     * in canonical form. In case of a method without parameters it will return an empty
     * array.
     *
     * <p>The canonical form is the one that is used in deployment descriptors.
     *
     * <p>Example: For the method <code>f(String[] arg0, String arg1, int)</code> this method will return
     * <code>{"java.lang.String[]", "java.lang.String", "int"}</code>
     *
     * @param viewMethod the method to extract its parameter types
     * @return string array of parameter types
     */
    public static String[] getCanonicalParameterTypes(Method viewMethod) {
        Class<?>[] parameterTypes = viewMethod.getParameterTypes();
        if (parameterTypes.length == 0) {
            return EMPTY_STRING_ARRAY;
        }
        String[] canonicalNames = new String[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            canonicalNames[i] = parameterTypes[i].getCanonicalName();
        }
        return canonicalNames;
    }

}
