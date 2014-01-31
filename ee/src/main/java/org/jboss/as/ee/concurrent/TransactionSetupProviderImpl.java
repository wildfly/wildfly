/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.concurrent;

import org.glassfish.enterprise.concurrent.spi.TransactionHandle;
import org.glassfish.enterprise.concurrent.spi.TransactionSetupProvider;
import org.jboss.as.ee.EeLogger;
import org.jboss.as.ee.EeMessages;
import org.jboss.as.ee.concurrent.service.ConcurrentServiceNames;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;

import javax.enterprise.concurrent.ManagedTask;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.io.ObjectStreamException;
import java.security.AccessController;

/**
 * The transaction setup provider handles transaction suspend/resume.
 *
 * @author Eduardo Martins
 */
public class TransactionSetupProviderImpl implements TransactionSetupProvider {

    private final transient TransactionManager transactionManager;

    /**
     * @param transactionManager
     */
    public TransactionSetupProviderImpl(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public TransactionHandle beforeProxyMethod(String transactionExecutionProperty) {
        Transaction transaction = null;
        if (!ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD.equals(transactionExecutionProperty)) {
            try {
                transaction = transactionManager.suspend();
            } catch (Throwable e) {
                EeLogger.ROOT_LOGGER.debug("failed to suspend transaction",e);
            }
        }
        return new TransactionHandleImpl(transaction);
    }

    @Override
    public void afterProxyMethod(TransactionHandle transactionHandle, String transactionExecutionProperty) {
        final Transaction transaction = ((TransactionHandleImpl) transactionHandle).getTransaction();
        if (transaction != null) {
            try {
                transactionManager.resume(transaction);
            } catch (Throwable e) {
                EeLogger.ROOT_LOGGER.debug("failed to resume transaction",e);
            }
        }
    }

    private static class TransactionHandleImpl implements TransactionHandle {

        private final Transaction transaction;

        private TransactionHandleImpl(Transaction transaction) {
            this.transaction = transaction;
        }

        public Transaction getTransaction() {
            return transaction;
        }
    }

    // serialization

    private Object readResolve() throws ObjectStreamException {
        final ServiceController<?> serviceController = currentServiceContainer().getService(ConcurrentServiceNames.TRANSACTION_SETUP_PROVIDER_SERVICE_NAME);
        if(serviceController == null) {
            throw EeMessages.MESSAGES.transactionSetupProviderServiceNotInstalled();
        }
        return serviceController.getValue();
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
