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

package org.jboss.as.naming;

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jnp.interfaces.Naming;
import org.jnp.interfaces.NamingContext;
import org.jnp.server.NamingServer;

import javax.naming.Context;
import javax.naming.spi.NamingManager;

/**
 * Service responsible for creating and managing the life-cycle of the Naming Server. 
 *
 * @author John E. Bailey
 */
public class NamingService implements Service<Naming> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("naming");
    private static final Logger log = Logger.getLogger("org.jboss.as.naming");
    private static final String PACKAGE_PREFIXES = "org.jboss.naming:org.jnp.interfaces";
    private NamingServer server;

    /**
     * Creates a new NamingServer and sets the naming context to use the naming server.
     *
     * @param context The start context
     * @throws StartException If any errors occur setting up the naming server
     */
    public synchronized void start(StartContext context) throws StartException {
        log.info("Starting Naming Service");
        try {
            server = new NamingServer();
            System.setProperty(Context.URL_PKG_PREFIXES, PACKAGE_PREFIXES);
            NamingManager.setInitialContextFactoryBuilder(new InitialContextFactoryBuilder());
            NamingContext.setLocal(server);
        } catch (Throwable t) {
            throw new StartException("Failed to start naming server", t);
        }
    }

    /**
     * Removes the naming server from the naming context.  
     *
     * @param context The stop context.
     */
    public synchronized void stop(StopContext context) {
        NamingContext.setLocal(null);
        server = null;
    }

    /**
     * Get the naming server.
     *
     * @return The naming server.
     * @throws IllegalStateException
     */
    public synchronized Naming getValue() throws IllegalStateException {
        return server;  
    }
}
