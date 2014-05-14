/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ee.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.ee.logging.EeLogger;

/**
 * Utility class for working with method descriptors
 *
 * @author Stuart Douglas
 */
public class DescriptorUtils {


    private static final Map<Class<?>, String> primitives;

    static {
        Map<Class<?>, String> p = new IdentityHashMap<Class<?>, String>();
        p.put(void.class, "V");
        p.put(byte.class, "B");
        p.put(char.class, "C");
        p.put(double.class, "D");
        p.put(float.class, "F");
        p.put(int.class, "I");
        p.put(long.class, "J");
        p.put(short.class, "S");
        p.put(boolean.class, "Z");
        primitives = Collections.unmodifiableMap(p);
    }

    /**
     * Changes a class name to the internal form suitable for use in a descriptor string.
     * <p/>
     * e.g. java.lang.String => Ljava/lang/String;
     */
    public static String makeDescriptor(String className) {
        String repl = className.replace(".", "/");
        return 'L' + repl + ';';
    }

    public static String makeDescriptor(Class<?> c) {
        String primitive = primitives.get(c);
        if(primitive != null) {
            return primitive;
        }else if (c.isArray()) {
            return c.getName().replace(".", "/");
        } else {
            return makeDescriptor(c.getName());
        }
    }

    public static String makeDescriptor(Constructor<?> c) {
        StringBuilder desc = new StringBuilder("(");
        for (Class<?> p : c.getParameterTypes()) {
            desc.append(DescriptorUtils.makeDescriptor(p));
        }
        desc.append(")");
        desc.append("V");
        return desc.toString();
    }

    /**
     * returns an array of String representations of the parameter types. Primitives are returned as their native
     * representations, while classes are returned in the internal descriptor form e.g. Ljava/lang/Integer;
     */
    public static String[] parameterDescriptors(String methodDescriptor) {
        int i = 1; // char 0 is a '('
        List<String> ret = new ArrayList<String>();
        int arrayStart = -1;
        while (methodDescriptor.charAt(i) != ')') {
            String type = null;
            if (methodDescriptor.charAt(i) == '[') {
                if (arrayStart == -1) {
                    arrayStart = i;
                }
            } else {
                if (methodDescriptor.charAt(i) == 'L') {
                    int start = i;
                    i++;
                    while (methodDescriptor.charAt(i) != ';') {
                        ++i;
                    }
                    if (arrayStart == -1) {
                        type = methodDescriptor.substring(start, i);
                    } else {
                        type = methodDescriptor.substring(arrayStart, i);
                    }
                } else {
                    if (arrayStart == -1) {
                        type = methodDescriptor.charAt(i) + "";
                    } else {
                        type = methodDescriptor.substring(arrayStart, i + 1);
                    }
                }
                arrayStart = -1;
                ret.add(type);
            }
            ++i;
        }
        String[] r = new String[ret.size()];
        for (int j = 0; j < ret.size(); ++j) {
            r[j] = ret.get(j);
        }
        return r;
    }

    public static String[] parameterDescriptors(Method m) {
        return parameterDescriptors(m.getParameterTypes());
    }

    public static String[] parameterDescriptors(Class<?>[] parameters) {
        String[] ret = new String[parameters.length];
        for (int i = 0; i < ret.length; ++i) {
            ret[i] = DescriptorUtils.makeDescriptor(parameters[i]);
        }
        return ret;
    }

    public static String returnType(String methodDescriptor) {
        return methodDescriptor.substring(methodDescriptor.lastIndexOf(')') + 1, methodDescriptor.length());
    }


    /**
     * returns true if the descriptor represents a primitive type
     */
    public static boolean isPrimitive(String descriptor) {
        if (descriptor.length() == 1) {
            return true;
        }
        return false;
    }

    public static String methodDescriptor(Method m) {
        StringBuilder desc = new StringBuilder("(");
        for (Class<?> p : m.getParameterTypes()) {
            desc.append(DescriptorUtils.makeDescriptor(p));
        }
        desc.append(")");
        desc.append(DescriptorUtils.makeDescriptor(m.getReturnType()));
        return desc.toString();
    }

    public static String methodDescriptor(String[] parameters, String returnType) {
        StringBuilder desc = new StringBuilder("(");
        for (String p : parameters) {
            desc.append(p);
        }
        desc.append(")");
        desc.append(returnType);
        return desc.toString();
    }

    /**
     * performs basic validation on a descriptor
     */
    public static String validateDescriptor(String descriptor) {
        if (descriptor.length() == 0) {
            throw EeLogger.ROOT_LOGGER.cannotBeEmpty("descriptors");
        }
        if (descriptor.length() > 1) {
            if (descriptor.startsWith("L")) {
                if (!descriptor.endsWith(";")) {
                    throw EeLogger.ROOT_LOGGER.invalidDescriptor(descriptor);
                }
            } else if (descriptor.startsWith("[")) {

            } else {
                    throw EeLogger.ROOT_LOGGER.invalidDescriptor(descriptor);
            }
        } else {
            char type = descriptor.charAt(0);
            switch (type) {
                case 'I':
                case 'Z':
                case 'S':
                case 'B':
                case 'F':
                case 'D':
                case 'V':
                case 'J':
                case 'C':
                    break;
                default:
                    throw EeLogger.ROOT_LOGGER.invalidDescriptor(descriptor);
            }
        }
        return descriptor;
    }
}
