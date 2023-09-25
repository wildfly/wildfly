/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
