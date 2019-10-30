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

import static org.wildfly.extension.messaging.activemq.logging.MessagingLogger.ROOT_LOGGER;

import java.io.Serializable;
import java.util.UUID;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSPasswordCredential;
import javax.jms.JMSSessionMode;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
class InjectedJMSContext extends JMSContextWrapper implements Serializable {

    private static final String TRANSACTION_SYNCHRONIZATION_REGISTRY_LOOKUP = "java:comp/TransactionSynchronizationRegistry";

    // Metadata to create the actual JMSContext.
    private final JMSInfo info;
    // JMSContext bean with the @RequestedScope
    private final RequestedJMSContext requestedJMSContext;
    // Identifier of the injected JMSContext
    private final String id;
    // JMSContext bean with the @TransactionScoped scope.
    // An indirect reference is used as the instance is only valide when the transaction scope is active.
    private transient Instance<TransactedJMSContext> transactedJMSContext;

    // Cached reference to the connectionFactory used to create the actual JMSContext.
    // It is cached to avoid repeated JNDI lookups.
    private transient ConnectionFactory connectionFactory;
    // Cached reference to the transaction sync registry to determine if a transaction is active
    private transient TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @Inject
    InjectedJMSContext(InjectionPoint ip, RequestedJMSContext requestedJMSContext, Instance<TransactedJMSContext> transactedJMSContext) {
        this.id = UUID.randomUUID().toString();
        this.requestedJMSContext = requestedJMSContext;
        this.transactedJMSContext = transactedJMSContext;
        JMSConnectionFactory connectionFactory = ip.getAnnotated().getAnnotation(JMSConnectionFactory.class);
        JMSPasswordCredential credential = ip.getAnnotated().getAnnotation(JMSPasswordCredential.class);
        JMSSessionMode sessionMode = ip.getAnnotated().getAnnotation(JMSSessionMode.class);

        this.info = new JMSInfo(connectionFactory, credential, sessionMode);
    }

    /**
     * Return the actual JMSContext used by this injection.
     *
     * The use of the correct AbstractJMSContext (one with the @RequestScoped, the other
     * with the @TransactionScoped) is determined by the presence on an active transaction.
     */
    @Override
    JMSContext getDelegate() {
        boolean inTx = isInTransaction();
        AbstractJMSContext jmsContext = inTx ? transactedJMSContext.get() : requestedJMSContext;

        ROOT_LOGGER.debugf("using %s to create the injected JMSContext", jmsContext, id);
        ConnectionFactory connectionFactory = getConnectionFactory();
        JMSContext contextInstance = jmsContext.getContext(id, info, connectionFactory);

        //fix of  WFLY-9501
        // CCM tries to clean opened connections before execution of @PreDestroy method on JMSContext - which is executed after completion, see .
        // Correct phase to call close is afterCompletion {@see TransactionSynchronizationRegistry.registerInterposedSynchronization}
        if(inTx) {
            TransactedJMSContext transactedJMSContext = (TransactedJMSContext)jmsContext;
            transactedJMSContext.registerCleanUpListener(transactionSynchronizationRegistry, contextInstance);
        }

        return contextInstance;
    }

    /**
     * check whether there is an active transaction.
     */
    private boolean isInTransaction() {
        TransactionSynchronizationRegistry tsr = getTransactionSynchronizationRegistry();
        boolean inTx = tsr.getTransactionStatus() == Status.STATUS_ACTIVE;
        return inTx;
    }

    /**
     * lookup the transactionSynchronizationRegistry and cache it.
     */
    private TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        TransactionSynchronizationRegistry cachedTSR = transactionSynchronizationRegistry;
        if (cachedTSR == null) {
            cachedTSR = (TransactionSynchronizationRegistry) lookup(TRANSACTION_SYNCHRONIZATION_REGISTRY_LOOKUP);
            transactionSynchronizationRegistry = cachedTSR;
        }
        return cachedTSR;
    }

    /**
     * lookup the connectionFactory and cache it.
     */
    private ConnectionFactory getConnectionFactory() {
        ConnectionFactory cachedCF = connectionFactory;

        if (cachedCF == null) {
            cachedCF = (ConnectionFactory)lookup(info.getConnectionFactoryLookup());
            connectionFactory = cachedCF;
        }
        return cachedCF;
    }

    private Object lookup(String name) {
        Context ctx = null;
        try {
            ctx = new InitialContext();
            return ctx.lookup(name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                }
            }
        }
    }
}
