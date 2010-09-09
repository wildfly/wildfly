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

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import static org.jboss.as.naming.util.NamingUtils.cast;

/**
 * Service wrapper for the java: naming context.  Mainly used as a dependency for other contexts and binders.
 *
 * @author John E. Bailey
 */
public class JavaContextService implements Service<Context> {
    public static final ServiceName SERVICE_NAME = NamingService.SERVICE_NAME.append("context", "java");
    private Context javaContext;

    /**
     * Looks up the java: context from the initial context.
     *
     * @param context The start context
     * @throws StartException If any naming errors occur getting the java: context
     */
    public synchronized void start(StartContext context) throws StartException {
        final Context initContext;
        try {
            initContext = new InitialContext();
        } catch (NamingException e) {
            throw new StartException("Failed to get initial context", e);
        }
        try {
            this.javaContext = cast(initContext.lookup("java:"));
        } catch (NamingException e) {
            throw new StartException("Failed to retrieve java: context", e);
        }
    }

    /**
     * Clear out the java: context.
     *
     * @param context The stop context.
     */
    public synchronized void stop(StopContext context) {
        this.javaContext = null;
    }

    /**
     * Get the java: context.
     *
     * @return The context
     * @throws IllegalStateException If the context has not been set
     */
    public synchronized Context getValue() throws IllegalStateException {
        if(javaContext == null) {
            throw new IllegalStateException("java: is null.  Has the service started");
        }
        return javaContext;
    }
}
