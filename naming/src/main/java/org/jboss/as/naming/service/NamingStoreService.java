/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.service;

import javax.naming.NamingException;

import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.WritableServiceBasedNamingStore;
import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service responsible for managing the creation and life-cycle of a service based naming store.
 *
 * @author John E. Bailey
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public class NamingStoreService implements Service<ServiceBasedNamingStore> {

    private final boolean readOnly;
    private volatile ServiceBasedNamingStore store;

    public NamingStoreService() {
        this(false);
    }

    public NamingStoreService(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Creates the naming store if not provided by the constructor.
     *
     * @param context The start context
     * @throws StartException If any problems occur creating the context
     */
    public void start(final StartContext context) throws StartException {
        if(store == null) {
            final ServiceRegistry serviceRegistry = context.getController().getServiceContainer();
            final ServiceName serviceNameBase = context.getController().getName();
            final ServiceTarget serviceTarget = context.getChildTarget();
            store = readOnly ? new ServiceBasedNamingStore(serviceRegistry, serviceNameBase) : new WritableServiceBasedNamingStore(serviceRegistry, serviceNameBase, serviceTarget);
        }
    }

    /**
     * Destroys the naming store.
     *
     * @param context The stop context
     */
    public void stop(StopContext context) {
        if(store != null) {
            try {
                store.close();
                store = null;
            } catch (NamingException e) {
                throw NamingLogger.ROOT_LOGGER.failedToDestroyRootContext(e);
            }
        }
    }

    /**
     * Get the context value.
     *
     * @return The naming store
     * @throws IllegalStateException
     */
    public ServiceBasedNamingStore getValue() throws IllegalStateException {
        return store;
    }
}
