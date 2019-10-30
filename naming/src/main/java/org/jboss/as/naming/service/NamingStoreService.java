/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
