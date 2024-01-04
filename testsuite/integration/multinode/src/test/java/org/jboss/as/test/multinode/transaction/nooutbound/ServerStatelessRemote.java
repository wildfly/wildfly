/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.transaction.nooutbound;

import java.rmi.RemoteException;

public interface ServerStatelessRemote {

    int transactionStatus() throws RemoteException;
}
