/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3;

/**
 * PrimitiveClassLoaderUtil
 * <p/>
 * Util for centralizing the logic for handling primitive types
 * during classloading. Use the {@link #loadClass(String, ClassLoader)}
 * to centralize the logic of checking for primitive types to ensure that
 * the {@link ClassLoader#loadClass(String)} method is not invoked for primitives.
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class PrimitiveClassLoaderUtil {

    /**
     * First checks if <code>name</code> is a primitive type. If yes, then returns
     * the corresponding {@link Class} for that primitive. If it's not a primitive
     * then the {@link Class#forName(String, boolean, ClassLoader)} method is invoked, passing
     * it the <code>name</code>, false and the <code>cl</code> classloader
     *
     * @param name The class that has to be loaded
     * @param cl   The {@link ClassLoader} to use, if <code>name</code> is *not* a primitive
     * @return Returns the {@link Class} corresponding to <code>name</code>
     * @throws ClassNotFoundException If the class for <code>name</code> could not be found
     * @see ClassLoader#loadClass(String)
     */
    public static Class<?> loadClass(String name, ClassLoader cl) throws ClassNotFoundException {
        /*
        * Handle Primitives
        */
        if (name.equals(void.class.getName())) {
            return void.class;
        }
        if (name.equals(byte.class.getName())) {
            return byte.class;
        }
        if (name.equals(short.class.getName())) {
            return short.class;
        }
        if (name.equals(int.class.getName())) {
            return int.class;
        }
        if (name.equals(long.class.getName())) {
            return long.class;
        }
        if (name.equals(char.class.getName())) {
            return char.class;
        }
        if (name.equals(boolean.class.getName())) {
            return boolean.class;
        }
        if (name.equals(float.class.getName())) {
            return float.class;
        }
        if (name.equals(double.class.getName())) {
            return double.class;
        }
        // Now that we know its not a primitive, lets just allow
        // the passed classloader to handle the request.
        // Note that we are intentionally using Class.forName(name,boolean,cl)
        // to handle issues with loading array types in Java 6 http://bugs.sun.com/view_bug.do?bug_id=6434149
        return Class.forName(name, false, cl);

    }
}
