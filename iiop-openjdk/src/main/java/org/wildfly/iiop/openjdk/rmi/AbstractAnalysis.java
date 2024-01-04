/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.rmi;


/**
 * Abstract base class for all analysis classes.
 * <p/>
 * Routines here are conforming to the "Java(TM) Language to IDL Mapping
 * Specification", version 1.1 (01-06-07).
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
abstract class AbstractAnalysis {

    /**
     * My unqualified IDL name.
     */
    private String idlName;

    /**
     * My unqualified java name.
     */
    private final String javaName;

    AbstractAnalysis(String idlName, String javaName) {
        this.idlName = idlName;
        this.javaName = javaName;
    }

    AbstractAnalysis(String javaName) {
        this(Util.javaToIDLName(javaName), javaName);
    }

    /**
     * Return my unqualified IDL name.
     */
    public String getIDLName() {
        return idlName;
    }

    /**
     * Return my unqualified java name.
     */
    public String getJavaName() {
        return javaName;
    }

    /**
     * Set my unqualified IDL name.
     */
    void setIDLName(String idlName) {
        this.idlName = idlName;
    }

}

