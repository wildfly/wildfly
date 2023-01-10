/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.ee.concurrent.handle;

import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.ManagedTask;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import org.jboss.as.ee.logging.EeLogger;
import org.wildfly.transaction.client.ContextTransactionManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * FIXME *FOLLOW UP* delete unused TransactionLeakContextHandleFactory and TransactionSetupProviderImpl, and deactivate unused logger msgs
 * A context handle factory which is responsible for handling the context type ContextServiceDefinition.TRANSACTION.
 *
 * @author Eduardo Martins
 */
public class TransactionContextHandleFactory implements EE10ContextHandleFactory {

    public static final String NAME = ContextServiceDefinition.TRANSACTION;

    @Override
    public String getContextType() {
        return NAME;
    }

    @Override
    public SetupContextHandle clearedContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        if (contextObjectProperties != null && ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD.equals(contextObjectProperties.get(ManagedTask.TRANSACTION))) {
            // override to unchanged
            return null;
        }
        return new ClearedSetupContextHandle(ContextTransactionManager.getInstance());
    }

    @Override
    public SetupContextHandle propagatedContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        // TODO *FOLLOW UP* not required by spec, but should we support it?!?
        return unchangedContext(contextService, contextObjectProperties);
    }

    @Override
    public SetupContextHandle unchangedContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        if (contextObjectProperties != null && ManagedTask.SUSPEND.equals(contextObjectProperties.get(ManagedTask.TRANSACTION))) {
            // override to cleared
            return new ClearedSetupContextHandle(ContextTransactionManager.getInstance());
        }
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getChainPriority() {
        // this should have a top priority since the legacy TransactionSetupProvider was executed before the contexthandlefactories
        return 10;
    }

    @Override
    public void writeSetupContextHandle(SetupContextHandle contextHandle, ObjectOutputStream out) throws IOException {
    }

    @Override
    public SetupContextHandle readSetupContextHandle(ObjectInputStream in) throws IOException, ClassNotFoundException {
        return new ClearedSetupContextHandle(ContextTransactionManager.getInstance());
    }

    private static class ClearedSetupContextHandle implements SetupContextHandle {

        private static final long serialVersionUID = 5751959084132309889L;
        private final TransactionManager transactionManager;

        private ClearedSetupContextHandle(TransactionManager transactionManager) {
            this.transactionManager = transactionManager;
        }

        @Override
        public ResetContextHandle setup() throws IllegalStateException {
            Transaction transactionOnSetup = null;
            if(transactionManager != null) {
                try {
                    transactionOnSetup = transactionManager.suspend();
                } catch (SystemException e) {
                    EeLogger.ROOT_LOGGER.failedToSuspendTransaction(e);
                }
            }
            return new ClearedResetContextHandle(transactionManager, transactionOnSetup);
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

    private static class ClearedResetContextHandle implements ResetContextHandle {

        private static final long serialVersionUID = 6601621971677254974L;
        private final TransactionManager transactionManager;
        private final Transaction transactionOnSetup;

        private ClearedResetContextHandle(TransactionManager transactionManager, Transaction transactionOnSetup) {
            this.transactionManager = transactionManager;
            this.transactionOnSetup = transactionOnSetup;
        }

        @Override
        public void reset() {
            try {
                transactionManager.resume(transactionOnSetup);
            } catch (Throwable e) {
                EeLogger.ROOT_LOGGER.failedToResumeTransaction(e);
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
