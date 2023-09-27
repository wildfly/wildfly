/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.sso.interfaces;

import java.rmi.RemoteException;

import jakarta.ejb.EJBObject;

/**
 * A trivial SessionBean interface.
 *
 * @author Scott.Stark@jboss.org
 */
public interface StatelessSession extends EJBObject {

    /** A method that returns its arg */
    String echo(String arg) throws RemoteException;

    /**
     * A method that does nothing. It is used to test call optimization.
     */
    void noop() throws RemoteException;

    /** Return a data object */
    ReturnData getData() throws RemoteException;

}
