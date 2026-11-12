/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.denylist;

import java.io.Serializable;
import java.rmi.RemoteException;
import jakarta.ejb.EJBObject;

/**
 * Remote interface for testing IIOP deny-list filter.
 */
public interface IIOPDenyListRemote extends EJBObject {

    /**
     * Method accepting a dangerous class that should be blocked by the deny-list filter.
     */
    String acceptDangerousClass(DangerousClass dangerous) throws RemoteException;

    /**
     * Method accepting a safe class that should pass the deny-list filter.
     */
    String acceptSafeClass(String safe) throws RemoteException;

    /**
     * Method accepting an arbitrary object, used to probe the built-in default deny-list with
     * classes that are already in the default filter spec (e.g. javax.swing.UIDefaults).
     */
    String acceptObject(Serializable object) throws RemoteException;
}
