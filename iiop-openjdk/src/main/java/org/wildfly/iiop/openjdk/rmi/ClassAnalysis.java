/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.rmi;


import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * Analysis class for classes. These define IDL types.
 * <p/>
 * Routines here are conforming to the "Java(TM) Language to IDL Mapping
 * Specification", version 1.1 (01-06-07).
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 */
public class ClassAnalysis extends AbstractAnalysis {


    /**
     * My java class.
     */
    protected Class cls;

    /**
     * Analyze the given class, and return the analysis.
     * public static ClassAnalysis getClassAnalysis(Class cls)
     * throws RMIIIOPViolationException
     * {
     * if (cls == null)
     * throw new IllegalArgumentException("Cannot analyze NULL class.");
     * if (cls == java.lang.String.class || cls == java.lang.Object.class     ||
     * cls == java.lang.Class.class  || cls == java.io.Serializable.class ||
     * cls == java.io.Externalizable.class ||
     * cls == java.rmi.Remote.class)
     * throw new IllegalArgumentException("Cannot analyze special class: " +
     * cls.getName());
     * <p/>
     * if (cls.isPrimitive())
     * return PrimitiveAnalysis.getPrimitiveAnalysis(cls);
     * <p/>
     * <p/>
     * if (cls.isInterface() && java.rmi.Remote.class.isAssignableFrom(cls))
     * return InterfaceAnalysis.getInterfaceAnalysis(cls);
     * // TODO
     * throw new RuntimeException("ClassAnalysis.getClassAnalysis() TODO");
     * }
     */

    private static String javaNameOfClass(Class cls) {
        if (cls == null)
            throw IIOPLogger.ROOT_LOGGER.cannotAnalyzeNullClass();

        String s = cls.getName();

        return s.substring(s.lastIndexOf('.') + 1);
    }


    public ClassAnalysis(Class cls, String idlName, String javaName) {
        super(idlName, javaName);

        this.cls = cls;
    }

    public ClassAnalysis(Class cls, String javaName) {
        this(cls, Util.javaToIDLName(javaName), javaName);
    }

    public ClassAnalysis(Class cls) {
        this(cls, javaNameOfClass(cls));
    }


    /**
     * Return my java class.
     */
    public Class getCls() {
        return cls;
    }
}

