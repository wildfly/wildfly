/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.handle;

import org.jboss.as.ee.logging.EeLogger;
import org.wildfly.transaction.client.ContextTransactionManager;

import jakarta.enterprise.concurrent.ContextService;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * A context handle factory which is responsible for preventing transaction leaks.
 *
 * @author Eduardo Martins
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class TransactionLeakContextHandleFactory implements ContextHandleFactory {

    public static final String NAME = "TRANSACTION_LEAK";

    @Override
    public SetupContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return new TransactionLeakSetupContextHandle(ContextTransactionManager.getInstance());
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
        return new TransactionLeakSetupContextHandle(ContextTransactionManager.getInstance());
    }

    private static class TransactionLeakSetupContextHandle implements SetupContextHandle {

        private static final long serialVersionUID = -8142799455606311295L;
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
            throw EeLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw EeLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }
    }

    private static class TransactionLeakResetContextHandle implements ResetContextHandle {

        private static final long serialVersionUID = -1726741825781759990L;
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
                                    EeLogger.ROOT_LOGGER.rollbackOfTransactionStartedInEEConcurrentInvocation();
                                    transactionManager.rollback();
                                } catch (Throwable e) {
                                    EeLogger.ROOT_LOGGER.failedToRollbackTransaction(e);
                                } finally {
                                    try {
                                        transactionManager.suspend();
                                    } catch (Throwable e) {
                                        EeLogger.ROOT_LOGGER.failedToSuspendTransaction(e);
                                    }
                                }
                        }
                    }

                } catch (SystemException e) {
                    EeLogger.ROOT_LOGGER.systemErrorWhileCheckingForTransactionLeak(e);
                }
            }
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }

        // serialization

        private void writeObject(ObjectOutputStream out) throws IOException {
            throw EeLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw EeLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }
    }
}
