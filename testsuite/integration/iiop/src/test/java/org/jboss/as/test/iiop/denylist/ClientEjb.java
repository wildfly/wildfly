/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.denylist;

import java.io.Serializable;
import java.rmi.RemoteException;
import jakarta.ejb.Stateless;

/**
 * Client EJB that invokes the IIOP server bean.
 * The home reference is injected via ejb-jar.xml using CORBA naming.
 */
@Stateless
public class ClientEjb {

    private IIOPDenyListHome home;

    public String invokeSafeMethod(String payload) throws RemoteException {
        return home.create().acceptSafeClass(payload);
    }

    public String invokeDangerousMethod(DangerousClass dangerous) throws RemoteException {
        return home.create().acceptDangerousClass(dangerous);
    }

    public String invokeObjectMethod(Serializable object) throws RemoteException {
        return home.create().acceptObject(object);
    }
}
