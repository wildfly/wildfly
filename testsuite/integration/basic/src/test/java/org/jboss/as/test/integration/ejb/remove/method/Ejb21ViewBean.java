/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remove.method;

import java.rmi.RemoteException;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.Stateful;

@Stateful
@RemoteHome(Ejb21ViewHome.class)
public class Ejb21ViewBean {
    // Class Members

    public static final String TEST_STRING = "Test";

    // Required Implementations

    public String test() throws RemoteException {
        return Ejb21ViewBean.TEST_STRING;
    }

    public void ejbCreate() {
    }
}
