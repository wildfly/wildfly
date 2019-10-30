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

package org.jboss.as.ejb3.util;

import java.lang.reflect.Method;

/**
 * This helper class contains helper methods that are used
 * to resolve method-params in deployment descriptors and method level annotations
 * in EJB implementation classes.
 *
 * @author robert.panzer@me.com
 *
 */
public final class MethodInfoHelper {

    private static final String[] NO_STRINGS = new String[0];

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
     * @param viewMethod
     * @return
     */
    public static String[] getCanonicalParameterTypes(Method viewMethod) {
        Class<?>[] parameterTypes = viewMethod.getParameterTypes();
        if (parameterTypes == null) {
            return NO_STRINGS;
        }
        String[] canonicalNames = new String[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            canonicalNames[i] = parameterTypes[i].getCanonicalName();
        }
        return canonicalNames;
    }

}
