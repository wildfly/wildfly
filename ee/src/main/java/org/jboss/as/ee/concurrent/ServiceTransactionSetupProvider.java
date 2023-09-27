/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import org.glassfish.enterprise.concurrent.spi.TransactionHandle;
import org.glassfish.enterprise.concurrent.spi.TransactionSetupProvider;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessController;

/**
 * A wrapper for a {@link org.glassfish.enterprise.concurrent.spi.TransactionSetupProvider} stored in a service, allowing deserialization through MSC.
 * @author emmartins
 */
public class ServiceTransactionSetupProvider implements TransactionSetupProvider {

    private transient TransactionSetupProvider transactionSetupProvider;
    private final ServiceName serviceName;

    /**
     *
     * @param transactionSetupProvider the provider to wrap
     * @param serviceName the name of the service where the provider may be retrieved
     */
    public ServiceTransactionSetupProvider(TransactionSetupProvider transactionSetupProvider, ServiceName serviceName) {
        this.transactionSetupProvider = transactionSetupProvider;
        this.serviceName = serviceName;
    }

    @Override
    public TransactionHandle beforeProxyMethod(String transactionExecutionProperty) {
        return transactionSetupProvider.beforeProxyMethod(transactionExecutionProperty);
    }

    @Override
    public void afterProxyMethod(TransactionHandle handle, String transactionExecutionProperty) {
        transactionSetupProvider.afterProxyMethod(handle, transactionExecutionProperty);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // resolve from msc
        final ServiceContainer currentServiceContainer = System.getSecurityManager() == null ? CurrentServiceContainer.getServiceContainer() : AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
        final ServiceController<?> serviceController = currentServiceContainer.getService(serviceName);
        if (serviceController == null) {
            throw EeLogger.ROOT_LOGGER.transactionSetupProviderServiceNotInstalled();
        }
        transactionSetupProvider = (TransactionSetupProvider) serviceController.getValue();
    }
}
