/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.transaction.nooutbound;

import jakarta.annotation.Resource;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.transaction.TransactionSynchronizationRegistry;

@Remote(ServerStatelessRemote.class)
@Stateless
public class ServerNeverStatelessBean implements ServerStatelessRemote {

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public int transactionStatus() {
        return transactionSynchronizationRegistry.getTransactionStatus();
    }

}
