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
package org.jboss.as.ee.concurrent.handle;

import org.jboss.as.ee.EeLogger;
import org.jboss.as.ee.EeMessages;
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
    public ContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return new TransactionLeakContextHandle(transactionManager);
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
    public void writeHandle(ContextHandle contextHandle, ObjectOutputStream out) throws IOException {
    }

    @Override
    public ContextHandle readHandle(ObjectInputStream in) throws IOException, ClassNotFoundException {
        return new TransactionLeakContextHandle(transactionManager);
    }

    @Override
    public void inject(TransactionManager value) throws InjectionException {
        transactionManager = value;
    }

    @Override
    public void uninject() {
        transactionManager = null;
    }

    private static class TransactionLeakContextHandle implements ContextHandle {

        private final TransactionManager transactionManager;
        private Transaction transactionOnSetup;

        private TransactionLeakContextHandle(TransactionManager transactionManager) {
            this.transactionManager = transactionManager;
        }

        @Override
        public void setup() throws IllegalStateException {
            if(transactionManager != null) {
                try {
                    transactionOnSetup = transactionManager.getTransaction();
                } catch (SystemException e) {
                    throw new IllegalStateException(e);
                }
            }
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
            throw EeMessages.MESSAGES.serializationMustBeHandledByThefactory();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw EeMessages.MESSAGES.serializationMustBeHandledByThefactory();
        }
    }
}
