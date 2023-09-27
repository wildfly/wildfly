/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.transaction;

import java.rmi.RemoteException;

/**
 * @author Stuart Douglas
 */
public interface TransactionalRemote {

    int transactionStatus() throws RemoteException;
}
