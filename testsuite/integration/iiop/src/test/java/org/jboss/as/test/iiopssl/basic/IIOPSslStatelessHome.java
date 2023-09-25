/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiopssl.basic;

import jakarta.ejb.EJBHome;
import java.rmi.RemoteException;

/**
 * @author Bartosz Spyrko-Smietanko
 */
public interface IIOPSslStatelessHome extends EJBHome {

    IIOPSslStatelessRemote create() throws RemoteException;

}
