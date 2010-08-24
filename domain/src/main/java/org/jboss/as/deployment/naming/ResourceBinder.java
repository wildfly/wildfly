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

package org.jboss.as.deployment.naming;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * Service responsible for binding and unbinding a resource into a naming context.  This service should be used as a dependency for
 * any service that needs to retrieve this entry from the context.
 *
 * @author John E. Bailey
 */
public class ResourceBinder<T> implements Service<Object> {
    public static final ServiceName JAVA_BINDER = ContextNames.JAVA_CONTEXT_SERVICE_NAME.append("binder");
    public static final ServiceName APPLICATION_BINDER = ContextNames.APPLICATION_CONTEXT_SERVICE_NAME.append("binder");
    public static final ServiceName MODULE_BINDER = ContextNames.MODULE_CONTEXT_SERVICE_NAME.append("binder");
    public static final ServiceName COMPONENT_BINDER = ContextNames.COMPONENT_CONTEXT_SERVICE_NAME.append("binder");

    private final InjectedValue<Context> namingContextValue = new InjectedValue<Context>();
    private final JndiName name;
    private final Value<T> value;

    /**
     * Construct  new instance.
     *
     * @param name The JNDI name to use for binding.
     * @param value The value to bind into JNDI
     */
    public ResourceBinder(final JndiName name, final Value<T> value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Bind the entry into the injected context.
     *
     * @param context The start context
     * @throws StartException If the entty can not be bound
     */
    public synchronized void start(StartContext context) throws StartException {
        final Context namingContext = namingContextValue.getValue();
        try {
            namingContext.rebind(name.getLocalName(), value.getValue());
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
            namingContext.unbind(name.getLocalName());
        } catch (NamingException e) {
            throw new RuntimeException("Failed to unbind resource from context [" + namingContext + "] at location [" + name + "]", e);
        }
    }

    /**
     * Get the value from the injected context.
     *
     * @return The value of the named entry
     * @throws IllegalStateException
     */
    public synchronized Object getValue() throws IllegalStateException {
        final Context namingContext = namingContextValue.getValue();
        try {
            return namingContext.lookup(name.getLocalName());
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
