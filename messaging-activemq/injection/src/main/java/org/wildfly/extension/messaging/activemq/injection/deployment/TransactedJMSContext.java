/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.injection.deployment;

import jakarta.jms.JMSContext;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionScoped;
import jakarta.transaction.TransactionSynchronizationRegistry;

import static org.wildfly.extension.messaging.activemq.injection._private.MessagingLogger.ROOT_LOGGER;


/**
 * Injection of JMSContext in the @TransactionScoped scope.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
@TransactionScoped
class TransactedJMSContext extends AbstractJMSContext {

    /**
     * Closing of transaction scoped JMSContext is executed through Synchronization listener.
     * This method registers listener, which takes care of closing JMSContext.
     *
     * @param transactionSynchronizationRegistry
     * @param contextInstance
     */
    void registerCleanUpListener(TransactionSynchronizationRegistry transactionSynchronizationRegistry, JMSContext contextInstance) {
        //to avoid registration of more listeners for one context, flag in transaction is used.
        Object alreadyRegistered = transactionSynchronizationRegistry.getResource(contextInstance);
        if (alreadyRegistered == null) {
            transactionSynchronizationRegistry.registerInterposedSynchronization(new AfterCompletionSynchronization(contextInstance));
            transactionSynchronizationRegistry.putResource(contextInstance, AfterCompletionSynchronization.class.getName());
        }
    }

    /**
     * Synchronization task, which executes "cleanup" on JMSContext in AfterCompletion phase. BeforeCompletion does nothing.
     */
    private class AfterCompletionSynchronization implements Synchronization {
        final JMSContext context;

        public AfterCompletionSynchronization(JMSContext context) {
            this.context = context;
        }

        @Override
        public void beforeCompletion() {
            //intentionally blank
        }

        @Override
        public void afterCompletion(int status) {
            ROOT_LOGGER.debugf("Clean up JMSContext created from %s", TransactedJMSContext.this);
            context.close();
        }
    }
}
