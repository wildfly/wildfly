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
package org.jboss.as.model.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ClassLoaderDebugUtil {

    public static void outputClass(Class<?> clazz) {
        System.out.println("****** Class " + clazz + " " + clazz.getClassLoader() + " *******");
        System.out.println("\nFields:");
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            System.out.println("    --" + field.getName() + "-" + field.getType().getName() + " (" + field.getType().getClassLoader() + ")");
        }
        System.out.println("\nConstructors");
        Constructor<?>[] ctors = clazz.getDeclaredConstructors();
        for (Constructor<?> ctor : ctors) {
            System.out.println("    --constructor [");
            for (Class<?> param : ctor.getParameterTypes()) {
                System.out.println("          " + param.getClass().getName() + " (" + param.getClass().getClassLoader() + ")");
            }
            System.out.println("      ]");
        }
        System.out.println("\nMethods:");
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            System.out.println("      " + method.getName() + " [");
            for (Class<?> param : method.getParameterTypes()) {
                System.out.println("        --" + param.getClass().getName() + " (" + param.getClass().getClassLoader());
            }
            System.out.println("      ]");
            System.out.println("      " + method.getReturnType().getName() + " (" + method.getReturnType().getClassLoader() + ")");
        }

    }

}
