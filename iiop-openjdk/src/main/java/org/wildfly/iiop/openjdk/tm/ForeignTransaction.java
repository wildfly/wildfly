/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.iiop.openjdk.tm;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
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
