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

package org.jboss.as.naming.deployment;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * Service responsible for managing the creation and life-cycle of a naming context.
 *
 * @author John E. Bailey
 */
public class ContextService implements Service<Context> {
    private final InjectedValue<Context> parentContextValue = new InjectedValue<Context>();
    private final JndiName name;
    private Context context;

    /**
     * Create an instance with a specific name.
     *
     * @param name The context name in the parent context
     */
    public ContextService(final JndiName name) {
        this.name = name;
    }

    /**
     * Creates a sub-context in the parent context with the provided name.
     *
     * @param context The start context
     * @throws StartException If any problems occur creating the context
     */
    public synchronized void start(final StartContext context) throws StartException {
        final Context parentContext = parentContextValue.getValue();
        try {
            this.context = parentContext.createSubcontext(name.getLocalName());
        } catch (NamingException e) {
            throw new StartException("Failed to create sub-context with name '" + name + "' in context '" + parentContext + "'", e);
        }
    }

    /**
     * Unbinds the context from the parent.
     *
     * @param context The stop context
     */
    public synchronized void stop(StopContext context) {
        final Context parentContext = parentContextValue.getValue();
        try {
            parentContext.destroySubcontext(name.getLocalName());
        } catch (NamingException e) {
            throw new IllegalStateException("Failed to destroy sub-context with name '" + name + "' from context '" + parentContext + "'", e);
        }
    }

    /**
     * Get the context value.
     *
     * @return The context
     * @throws IllegalStateException
     */
    public synchronized Context getValue() throws IllegalStateException {
        return context;
    }

    /**
     * Get the parent context injector.
     *
     * @return The injector
     */
    public Injector<Context> getParentContextInjector() {
        return parentContextValue;
    }
}
