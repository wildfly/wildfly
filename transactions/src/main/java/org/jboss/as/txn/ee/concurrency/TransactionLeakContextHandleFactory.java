/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.txn.ee.concurrency;

import org.jboss.as.ee.concurrent.handle.ContextHandleFactory;
import org.jboss.as.ee.concurrent.handle.ResetContextHandle;
import org.jboss.as.ee.concurrent.handle.SetupContextHandle;
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;

import javax.enterprise.concurrent.ContextService;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * A context handle factory which is responsible for preventing transaction leaks.
 *
 * @author Eduardo Martins
 */
public class TransactionLeakContextHandleFactory implements ContextHandleFactory, Injector<TransactionManager> {

    public static final String NAME = "TRANSACTION_LEAK";

    private TransactionManager transactionManager;

    @Override
    public SetupContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return new TransactionLeakSetupContextHandle(transactionManager);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getChainPriority() {
        // must be higher/after other ee setup actions, which include the connector invocation context setup
        return 600;
    }

    @Override
    public void writeSetupContextHandle(SetupContextHandle contextHandle, ObjectOutputStream out) throws IOException {
    }

    @Override
    public SetupContextHandle readSetupContextHandle(ObjectInputStream in) throws IOException, ClassNotFoundException {
        return new TransactionLeakSetupContextHandle(transactionManager);
    }

    @Override
    public void inject(TransactionManager value) throws InjectionException {
        transactionManager = value;
    }

    @Override
    public void uninject() {
        transactionManager = null;
    }

    private static class TransactionLeakSetupContextHandle implements SetupContextHandle {

        private final TransactionManager transactionManager;

        private TransactionLeakSetupContextHandle(TransactionManager transactionManager) {
            this.transactionManager = transactionManager;
        }

        @Override
        public ResetContextHandle setup() throws IllegalStateException {
            Transaction transactionOnSetup = null;
            if(transactionManager != null) {
                try {
                    transactionOnSetup = transactionManager.getTransaction();
                } catch (SystemException e) {
                    throw new IllegalStateException(e);
                }
            }
            return new TransactionLeakResetContextHandle(transactionManager, transactionOnSetup);
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }

        // serialization

        private void writeObject(ObjectOutputStream out) throws IOException {
            throw TransactionLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw TransactionLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }
    }

    private static class TransactionLeakResetContextHandle implements ResetContextHandle {

        private final TransactionManager transactionManager;
        private final Transaction transactionOnSetup;

        private TransactionLeakResetContextHandle(TransactionManager transactionManager, Transaction transactionOnSetup) {
            this.transactionManager = transactionManager;
            this.transactionOnSetup = transactionOnSetup;
        }

        @Override
        public void reset() {
            if(transactionManager != null) {
                try {
                    final Transaction transactionOnReset = transactionManager.getTransaction();
                    if(transactionOnReset != null && !transactionOnReset.equals(transactionOnSetup)) {
                        switch (transactionOnReset.getStatus()) {
                            case Status.STATUS_ACTIVE:
                            case Status.STATUS_COMMITTING:
                            case Status.STATUS_MARKED_ROLLBACK:
                            case Status.STATUS_PREPARING:
                            case Status.STATUS_ROLLING_BACK:
                            case Status.STATUS_PREPARED:
                                try {
                                    TransactionLogger.ROOT_LOGGER.rollbackOfTransactionStartedInEEConcurrentInvocation();
                                    transactionManager.rollback();
                                } catch (Throwable e) {
                                    TransactionLogger.ROOT_LOGGER.failedToRollbackTransaction(e);
                                } finally {
                                    try {
                                        transactionManager.suspend();
                                    } catch (Throwable e) {
                                        TransactionLogger.ROOT_LOGGER.failedToSuspendTransaction(e);
                                    }
                                }
                        }
                    }

                } catch (SystemException e) {
                    TransactionLogger.ROOT_LOGGER.systemErrorWhileCheckingForTransactionLeak(e);
                }
            }
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }

        // serialization

        private void writeObject(ObjectOutputStream out) throws IOException {
            throw TransactionLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw TransactionLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }
    }
}
