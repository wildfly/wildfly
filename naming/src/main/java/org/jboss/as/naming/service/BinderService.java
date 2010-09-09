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
import org.jboss.msc.value.Value;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * Service responsible for binding and unbinding a entry into a naming context.  This service can be used as a dependency for
 * any service that needs to retrieve this entry from the context.
 *
 * @author John E. Bailey
 */
public class BinderService<T> implements Service<T> {
    private final InjectedValue<Context> namingContextValue = new InjectedValue<Context>();
    private final String name;
    private final Value<T> value;

    /**
     * Construct new instance.
     *
     * @param name The JNDI name to use for binding.
     * @param value The value to bind into JNDI
     */
    public BinderService(final String name, final Value<T> value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Bind the entry into the injected context.
     *
     * @param context The start context
     * @throws StartException If the entity can not be bound
     */
    public synchronized void start(StartContext context) throws StartException {
        final Context namingContext = namingContextValue.getValue();
        try {
            namingContext.rebind(name, value.getValue());
        } catch (NamingException e) {
            throw new StartException("Failed to bind resource into context [" + namingContext + "] at location [" + name + "]", e);
        }
    }

    /**
     * Unbind the entry from the injected context.
     *
     * @param context The stop context
     */
    public synchronized void stop(StopContext context) {
        final Context namingContext = namingContextValue.getValue();
        try {
            namingContext.unbind(name);
        } catch (NamingException e) {
            throw new IllegalStateException("Failed to unbind resource from context [" + namingContext + "] at location [" + name + "]", e);
        }
    }

    /**
     * Get the value from the injected context.
     *
     * @return The value of the named entry
     * @throws IllegalStateException
     */
    @SuppressWarnings("unchecked")
    public synchronized T getValue() throws IllegalStateException {
        final Context namingContext = namingContextValue.getValue();
        try {
            return (T)namingContext.lookup(name);
        } catch (NamingException e) {
            throw new RuntimeException("Failed to lookup value from context [" + namingContext + "] at location [" + name + "]", e);
        }
    }

    /**
     * Get the naming context injector.
     *
     * @return the injector
     */
    public Injector<Context> getContextInjector() {
        return namingContextValue;
    }

}
