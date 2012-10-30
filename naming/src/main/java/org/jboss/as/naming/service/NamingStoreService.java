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
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import static org.jboss.as.naming.NamingMessages.MESSAGES;

/**
 * Service responsible for managing the creation and life-cycle of a naming store.
 * <p>
 * Contexts created by this service use a separate in-memory store
 *
 * @author John E. Bailey
 * @author Stuart Douglas
 */
public class NamingStoreService implements Service<ServiceBasedNamingStore> {
    private ServiceBasedNamingStore store;

    public NamingStoreService() {
    }

    public NamingStoreService(ServiceBasedNamingStore store) {
        this.store = store;
    }

    /**
     * Creates the naming store if not provided by the constructor.
     *
     * @param context The start context
     * @throws StartException If any problems occur creating the context
     */
    public synchronized void start(final StartContext context) throws StartException {
        if(store == null) {
            store = new WritableServiceBasedNamingStore(context.getController().getServiceContainer(), context.getController().getName(),context.getChildTarget());
        }
    }

    /**
     * Destroys the naming store.
     *
     * @param context The stop context
     */
    public synchronized void stop(StopContext context) {
        try {
            store.close();
            store = null;
        } catch (NamingException e) {
            throw MESSAGES.failedToDestroyRootContext(e);
        }
    }

    /**
     * Get the context value.
     *
     * @return The naming store
     * @throws IllegalStateException
     */
    public synchronized ServiceBasedNamingStore getValue() throws IllegalStateException {
        return store;
    }
}
