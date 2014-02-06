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

import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * Value that is looked up from a naming context.
 *
 * @author John E. Bailey
 */
public class NamingLookupValue<T> implements Value<T> {
    private final InjectedValue<Context> contextValue = new InjectedValue<Context>();
    private final String contextName;

    /**
     * Create a new instance.
     *
     * @param contextName The context name to lookup if the value is not injected
     */
    public NamingLookupValue(final String contextName) {
        this.contextName = contextName;
    }

    /**
     * Lookup the value from the naming context.
     *
     * @return the injected value if present, the value retrieved from the context if not.
     * @throws IllegalStateException The name is not found in the context when called
     */
    public T getValue() throws IllegalStateException {
        final Context context = contextValue.getValue();
        try {
            return (T)context.lookup(contextName);
        } catch (NamingException e) {
            throw NamingLogger.ROOT_LOGGER.entryNotRegistered(e, contextName, context);
        }
    }

    /**
     * Get the naming context injector.
     *
     * @return The context injector
     */
    public Injector<Context> getContextInjector() {
        return contextValue;
    }
}
