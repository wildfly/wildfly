/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.injection.deployment;


import java.io.Serializable;
import java.util.UUID;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSPasswordCredential;
import jakarta.jms.JMSSessionMode;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionSynchronizationRegistry;

import static org.wildfly.extension.messaging.activemq.injection._private.MessagingLogger.ROOT_LOGGER;

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
