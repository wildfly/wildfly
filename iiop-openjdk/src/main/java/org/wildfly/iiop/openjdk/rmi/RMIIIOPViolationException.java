/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.rmi;


/**
 * Exception denoting an RMI/IIOP subset violation.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 */
public class RMIIIOPViolationException extends Exception {

    /**
     * The section violated.
     */
    private final String section;

    public RMIIIOPViolationException(String msg) {
        this(msg, null);
    }

    public RMIIIOPViolationException(String msg, String section) {
        super(msg);
        this.section = section;
    }

    /**
     * Return the section violated.
     */
    public String getSection() {
        return section;
    }

}

