/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.rmi;


/**
 * Exception denoting a part of RMI/IIOP not yet implemented here.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 */
public class RMIIIOPNotImplementedException   extends RMIIIOPViolationException {

    public RMIIIOPNotImplementedException(String msg) {
        super(msg);
    }
}

