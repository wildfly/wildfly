/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.iiop.openjdk.rmi;


import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * Analysis class for primitive types.
 * <p/>
 * Routines here are conforming to the "Java(TM) Language to IDL Mapping
 * Specification", version 1.1 (01-06-07).
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 */
public class PrimitiveAnalysis extends ClassAnalysis {

    public static final PrimitiveAnalysis voidAnalysis = new PrimitiveAnalysis(Void.TYPE, "void", "void");
    public static final PrimitiveAnalysis booleanAnalysis = new PrimitiveAnalysis(Boolean.TYPE, "boolean", "boolean");
    public static final PrimitiveAnalysis charAnalysis = new PrimitiveAnalysis(Character.TYPE, "wchar", "char");
    public static final PrimitiveAnalysis byteAnalysis = new PrimitiveAnalysis(Byte.TYPE, "octet", "byte");
    public static final PrimitiveAnalysis shortAnalysis = new PrimitiveAnalysis(Short.TYPE, "short", "short");
    public static final PrimitiveAnalysis intAnalysis = new PrimitiveAnalysis(Integer.TYPE, "long", "int");
    public static final PrimitiveAnalysis longAnalysis = new PrimitiveAnalysis(Long.TYPE, "long_long", "long");
    public static final PrimitiveAnalysis floatAnalysis = new PrimitiveAnalysis(Float.TYPE, "float", "float");
    public static final PrimitiveAnalysis doubleAnalysis = new PrimitiveAnalysis(Double.TYPE, "double", "double");

    private PrimitiveAnalysis(final Class cls, final String idlName, final String javaName) {
        super(cls, idlName, javaName);
    }


    /**
     * Get a singleton instance representing one of the primitive types.
     */
    public static PrimitiveAnalysis getPrimitiveAnalysis(final Class cls) {
        if (cls == null)
            throw IIOPLogger.ROOT_LOGGER.cannotAnalyzeNullClass();

        if (cls == Void.TYPE)
            return voidAnalysis;
        if (cls == Boolean.TYPE)
            return booleanAnalysis;
        if (cls == Character.TYPE)
            return charAnalysis;
        if (cls == Byte.TYPE)
            return byteAnalysis;
        if (cls == Short.TYPE)
            return shortAnalysis;
        if (cls == Integer.TYPE)
            return intAnalysis;
        if (cls == Long.TYPE)
            return longAnalysis;
        if (cls == Float.TYPE)
            return floatAnalysis;
        if (cls == Double.TYPE)
            return doubleAnalysis;

        throw IIOPLogger.ROOT_LOGGER.notAPrimitive(cls.getName());
    }

}
