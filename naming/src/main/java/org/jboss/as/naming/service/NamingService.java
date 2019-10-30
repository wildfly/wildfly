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

package org.jboss.as.naming.service;

import org.jboss.as.naming.NamingContext;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import static org.jboss.as.naming.logging.NamingLogger.ROOT_LOGGER;

/**
 * Service responsible for creating and managing the life-cycle of the Naming Server.
 *
 * @author John E. Bailey
 * @author Eduardo Martins
 */
public class NamingService implements Service<NamingStore> {
    public static final String CAPABILITY_NAME = "org.wildfly.naming";
    /**
     * @deprecated Dependent subsystem should instead register a requirement on the {@link #CAPABILITY_NAME} capability.
     */
    @Deprecated
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("naming");
    private final InjectedValue<NamingStore> namingStore;

    /**
     * Construct a new instance.
     *
     */
    public NamingService() {
        this.namingStore = new InjectedValue<>();
    }

    /**
     * Retrieves the naming store's InjectedValue.
     * @return
     */
    public InjectedValue<NamingStore> getNamingStore() {
        return namingStore;
    }

    /**
     * Creates a new NamingServer and sets the naming context to use the naming server.
     *
     * @param context The start context
     * @throws StartException If any errors occur setting up the naming server
     */
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.startingService();
        try {
            NamingContext.setActiveNamingStore(namingStore.getValue());
        } catch (Throwable t) {
            throw new StartException(NamingLogger.ROOT_LOGGER.failedToStart("naming service"), t);
        }
    }

    /**
     * Removes the naming server from the naming context.
     *
     * @param context The stop context.
     */
    public void stop(StopContext context) {
        NamingContext.setActiveNamingStore(null);
    }

    /**
     * Get the naming store value.
     *
     * @return The naming store.
     * @throws IllegalStateException
     */
    public NamingStore getValue() throws IllegalStateException {
        return namingStore.getValue();
    }
}
