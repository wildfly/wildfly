/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq.deployment.injection;

import javax.jms.JMSContext;
import javax.transaction.Synchronization;
import javax.transaction.TransactionScoped;
import javax.transaction.TransactionSynchronizationRegistry;

import static org.wildfly.extension.messaging.activemq.logging.MessagingLogger.ROOT_LOGGER;

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
        Object alreadyRegistered = transactionSynchronizationRegistry.getResource(AfterCompletionSynchronization.class.getName());
        if (alreadyRegistered == null) {
            transactionSynchronizationRegistry.registerInterposedSynchronization(new AfterCompletionSynchronization(contextInstance));
            transactionSynchronizationRegistry.putResource(AfterCompletionSynchronization.class.getName(), contextInstance);
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
