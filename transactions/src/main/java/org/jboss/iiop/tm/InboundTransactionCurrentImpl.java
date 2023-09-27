/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.iiop.tm;

import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;

import org.omg.CORBA.LocalObject;
import org.wildfly.transaction.client.ContextTransactionManager;
import org.wildfly.transaction.client.LocalTransactionContext;

public class InboundTransactionCurrentImpl extends LocalObject implements InboundTransactionCurrent {

    private static final long serialVersionUID = - 7415245830690060507L;

    public Transaction getCurrentTransaction() {
        final LocalTransactionContext current = LocalTransactionContext.getCurrent();
        try {
            current.importProviderTransaction();
        } catch (SystemException e) {
            throw new RuntimeException("InboundTransactionCurrentImpl unable to determine inbound transaction context", e);
        }

        try {
            return ContextTransactionManager.getInstance().suspend();
        } catch (SystemException e) {
            throw new RuntimeException("InboundTransactionCurrentImpl unable to suspend inbound transaction context", e);
        }
    }
}
