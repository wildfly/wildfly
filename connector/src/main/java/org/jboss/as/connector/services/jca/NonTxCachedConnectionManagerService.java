/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.jca;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import javax.transaction.xa.XAResource;

import org.jboss.jca.core.api.connectionmanager.ConnectionManager;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.connectionmanager.ccm.CachedConnectionManagerImpl;
import org.jboss.jca.core.spi.recovery.RecoveryPlugin;
import org.jboss.jca.core.spi.security.SubjectFactory;
import org.jboss.jca.core.spi.transaction.ConnectableResource;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.core.spi.transaction.XAResourceStatistics;
import org.jboss.jca.core.spi.transaction.local.LocalXAResource;
import org.jboss.jca.core.spi.transaction.recovery.XAResourceRecovery;
import org.jboss.jca.core.spi.transaction.recovery.XAResourceRecoveryRegistry;
import org.jboss.jca.core.spi.transaction.usertx.UserTransactionRegistry;
import org.jboss.jca.core.spi.transaction.xa.XAResourceWrapper;
import org.jboss.jca.core.spi.transaction.xa.XATerminator;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Cached connection manager service
 */
public class NonTxCachedConnectionManagerService implements Service<CachedConnectionManager> {

    private volatile CachedConnectionManager value;

    private final boolean debug;
    private final boolean error;
    private final boolean ignoreUnknownConnections;

    /** create an instance **/
    public NonTxCachedConnectionManagerService(final boolean debug, final boolean error, boolean ignoreUnknownConnections) {
        super();
        this.debug = debug;
        this.error = error;
        this.ignoreUnknownConnections = ignoreUnknownConnections;
    }

    @Override
    public CachedConnectionManager getValue() throws IllegalStateException, IllegalArgumentException {
        return value;
    }

    @Override
    public void start(StartContext context) throws StartException {
        value = new CachedConnectionManagerImpl(new NoopTransactionIntegration());
        value.setDebug(debug);
        value.setError(error);
        value.setIgnoreUnknownConnections(ignoreUnknownConnections);

        value.start();
        ROOT_LOGGER.debugf("Started CcmService %s", context.getController().getName());
    }

    @Override
    public void stop(StopContext context) {
        value.stop();
        ROOT_LOGGER.debugf("Stopped CcmService %s", context.getController().getName());
    }

    /** No operation transaction integration */
    private static class NoopTransactionIntegration implements TransactionIntegration {

        @Override
        public TransactionManager getTransactionManager() {
            return null;
        }

        @Override
        public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
            return null;
        }

        @Override
        public UserTransactionRegistry getUserTransactionRegistry() {
            return null;
        }

        @Override
        public XAResourceRecoveryRegistry getRecoveryRegistry() {
            return null;
        }

        @Override
        public XATerminator getXATerminator() {
            return null;
        }

        @Override
        public XAResourceRecovery createXAResourceRecovery(ResourceAdapter rar, ActivationSpec as, String productName,
                String productVersion) {
            throw ROOT_LOGGER.noSupportedOperation("createXAResourceRecovery");
        }

        @Override
        public XAResourceRecovery createXAResourceRecovery(ManagedConnectionFactory mcf, Boolean pad, Boolean override,
                Boolean wrapXAResource, String recoverUserName, String recoverPassword, String recoverSecurityDomain,
                SubjectFactory subjectFactory, RecoveryPlugin plugin, XAResourceStatistics xastat) {
            throw ROOT_LOGGER.noSupportedOperation("createXAResourceRecovery-Security");
        }

        @Override
        public LocalXAResource createLocalXAResource(ConnectionManager cm, String productName, String productVersion,
                String jndiName, XAResourceStatistics xastat) {
            throw ROOT_LOGGER.noSupportedOperation("createLocalXAResource");
        }

        @Override
        public LocalXAResource createConnectableLocalXAResource(ConnectionManager cm, String productName,
                String productVersion, String jndiName, ConnectableResource cr, XAResourceStatistics xastat) {
            throw ROOT_LOGGER.noSupportedOperation("createConnectableLocalXAResource");
        }

        @Override
        public LocalXAResource createConnectableLocalXAResource(ConnectionManager cm, String productName,
                String productVersion, String jndiName, ManagedConnection mc, XAResourceStatistics xastat) {
            return null;
        }

        @Override
        public XAResourceWrapper createXAResourceWrapper(XAResource xares, boolean pad, Boolean override, String productName,
                String productVersion, String jndiName, boolean firstResource, XAResourceStatistics xastat) {
            throw ROOT_LOGGER.noSupportedOperation("createXAResourceWrapper");
        }

        @Override
        public XAResourceWrapper createConnectableXAResourceWrapper(XAResource xares, boolean pad, Boolean override,
                String productName, String productVersion, String jndiName, ConnectableResource cr, XAResourceStatistics xastat) {
            throw ROOT_LOGGER.noSupportedOperation("createConnectableXAResourceWrapper");
        }

        @Override
        public XAResourceWrapper createConnectableXAResourceWrapper(XAResource xares, boolean pad, Boolean override,
                String productName, String productVersion, String jndiName, ManagedConnection mc, XAResourceStatistics xastat) {
            throw ROOT_LOGGER.noSupportedOperation("createConnectableXAResourceWrapper");
        }

        @Override
        public boolean isFirstResource(ManagedConnection mc) {
            throw ROOT_LOGGER.noSupportedOperation("isFirstResource");
        }

        @Override
        public boolean isConnectableResource(ManagedConnection mc) {
            throw ROOT_LOGGER.noSupportedOperation("isConnectableResource");
        }

        @Override
        public Object getIdentifier(Transaction tx) {
            throw ROOT_LOGGER.noSupportedOperation("getIdentifier");
        }
    }

}
