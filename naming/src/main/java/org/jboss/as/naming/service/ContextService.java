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

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * Service responsible for creating and managing the life-cycle of a naming context.  Will create a sub-context with
 * the provided name in the injected parent context.
 *
 * @author John E. Bailey
 */
public class ContextService implements Service<Context> {
    private final InjectedValue<Context> parentContextValue = new InjectedValue<Context>();
    private final String contextName;
    private Context context;

    /**
     * Construct new instance.
     *
     * @param contextName The context name
     */
    public ContextService(String contextName) {
        this.contextName = contextName;
    }

    /**
     * Attempt to create a named sub-context using either the injected parent context or a new InitialContex if no
     * parent was injected.
     *
     * @param context The start context.
     * @throws StartException If any problems occur creating the sub-context.
     */
    public synchronized void start(StartContext context) throws StartException {
        try {
            final Context parentContext = parentContextValue.getValue();
            this.context = parentContext.createSubcontext(contextName);
        } catch (NamingException e) {
            throw new StartException("Failed to create context", e);
        }
    }

    /**
     * Unbind the context from the parent context.
     *
     * @param context The stop context
     * @throws IllegalStateException If the context has not been bound.
     */
    public synchronized void stop(StopContext context) {
        try {
            final Context parentContext = parentContextValue.getValue();
            parentContext.unbind(contextName);
        } catch (NamingException e) {
            throw new IllegalStateException("Failed to unbind context", e);
        }
    }

    /**
     * Get the naming context.
     *
     * @return The naming context
     */
    public Context getValue() throws IllegalStateException {
        return context;
    }

    /**
     * Get the parent naming context injector.
     *
     * @return The injector
     */
    public Injector<Context> getParentContextInjector() {
        return parentContextValue;
    }
}
