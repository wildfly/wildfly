/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.tm;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import javax.transaction.xa.XAResource;

import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * A ForeignTransaction, a marker for when we would have to import a
 * transaction from another vendor. Which we don't do at the moment.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 */
public class ForeignTransaction implements Transaction {
    public static final ForeignTransaction INSTANCE = new ForeignTransaction();

    private ForeignTransaction() {
    }

    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
            SecurityException, SystemException {
        throw IIOPLogger.ROOT_LOGGER.foreignTransaction();
    }

    public void rollback() throws IllegalStateException, SystemException {
        throw IIOPLogger.ROOT_LOGGER.foreignTransaction();
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        throw IIOPLogger.ROOT_LOGGER.foreignTransaction();
    }

    public int getStatus() throws SystemException {
        throw IIOPLogger.ROOT_LOGGER.foreignTransaction();
    }

    public boolean enlistResource(XAResource xaRes) throws RollbackException, IllegalStateException, SystemException {
        throw IIOPLogger.ROOT_LOGGER.foreignTransaction();
    }

    public boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
        throw IIOPLogger.ROOT_LOGGER.foreignTransaction();
    }

    public void registerSynchronization(Synchronization sync) throws RollbackException, IllegalStateException,
            SystemException {
        throw IIOPLogger.ROOT_LOGGER.foreignTransaction();
    }
}
