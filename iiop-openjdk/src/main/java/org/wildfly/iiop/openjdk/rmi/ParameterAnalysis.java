/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.rmi;

import org.omg.CORBA.ParameterMode;


/**
 * Parameter analysis.
 * <p/>
 * Routines here are conforming to the "Java(TM) Language to IDL Mapping
 * Specification", version 1.1 (01-06-07).
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 */
public class ParameterAnalysis extends AbstractAnalysis {

    /**
     * Java type of parameter.
     */
    private final Class cls;

    /**
     * IDL type name of parameter type.
     */
    private final String typeIDLName;

    ParameterAnalysis(final String javaName, final Class cls)  throws RMIIIOPViolationException {
        super(javaName);
        this.cls = cls;
        typeIDLName = Util.getTypeIDLName(cls);
    }


    /**
     * Return my attribute mode.
     */
    public ParameterMode getMode() {
        // 1.3.4.4 says we always map to IDL "in" parameters.
        return ParameterMode.PARAM_IN;
    }

    /**
     * Return my Java type.
     */
    public Class getCls() {
        return cls;
    }

    /**
     * Return the IDL type name of my parameter type.
     */
    public String getTypeIDLName() {
        return typeIDLName;
    }

}
