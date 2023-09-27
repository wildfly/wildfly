/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiopssl.basic;

import jakarta.ejb.EJBObject;
import java.rmi.RemoteException;

/**
 * @author Bartosz Spyrko-Smietanko
 */
public interface IIOPSslStatelessRemote extends EJBObject {

    String hello() throws RemoteException;

}
