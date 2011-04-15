/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.naming;

import javax.naming.NamingException;

import org.jboss.as.naming.InMemoryNamingStore;
import org.jboss.as.naming.NamingStore;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service responsible for managing the creation and life-cycle of a naming context.
 * <p>
 * Contexts created by this service use a separate in-memory store
 *
 * @author John E. Bailey
 * @author Stuart Douglas
 */
public class RootContextService implements Service<NamingStore> {
    private InMemoryNamingStore store;

    /**
     * Creates a sub-context in the parent context with the provided name.
     *
     * @param context The start context
     * @throws StartException If any problems occur creating the context
     */
    public synchronized void start(final StartContext context) throws StartException {
        store = new InMemoryNamingStore();
    }

    /**
     * Unbinds the context from the parent.
     *
     * @param context The stop context
     */
    public synchronized void stop(StopContext context) {
        try {
            store.close();
            store = null;
        } catch (NamingException e) {
            throw new IllegalStateException("Failed to destroy root context", e);
        }
    }

    /**
     * Get the context value.
     *
     * @return The naming store
     * @throws IllegalStateException
     */
    public synchronized NamingStore getValue() throws IllegalStateException {
        return store;
    }
}
