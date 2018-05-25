/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.jpa.hibernate5.service;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JtaSynchronizationStrategy;

/**
 * WildFlyCustomJtaPlatform
 *
 * @author Scott Marlow
 */
public class WildFlyCustomJtaPlatform extends JBossAppServerJtaPlatform {

    private static final String TSR_NAME = "java:jboss/TransactionSynchronizationRegistry";

    @Override
    protected JtaSynchronizationStrategy getSynchronizationStrategy() {
        final TransactionSynchronizationRegistry transactionSynchronizationRegistry =
                locateTransactionSynchronizationRegistry();

        return new JtaSynchronizationStrategy() {


            @Override
            public void registerSynchronization(Synchronization synchronization) {
                transactionSynchronizationRegistry.registerInterposedSynchronization(synchronization);
            }

            @Override
            public boolean canRegisterSynchronization() {
                return transactionSynchronizationRegistry.getTransactionStatus() == Status.STATUS_ACTIVE;
            }
        };

    }

    private TransactionSynchronizationRegistry locateTransactionSynchronizationRegistry() {
        return (TransactionSynchronizationRegistry) jndiService().locate(TSR_NAME);
    }

}
