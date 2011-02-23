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

import javax.naming.Name;
import javax.naming.Reference;
import org.jboss.as.naming.JndiInjectable;
import org.jboss.as.naming.JndiInjectableObjectFactory;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.util.NameParser;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.naming.NamingException;

/**
 * Service responsible for binding and unbinding a entry into a naming context.  This service can be used as a dependency for
 * any service that needs to retrieve this entry from the context.
 *
 * @author John E. Bailey
 */
public class BinderService implements Service<JndiInjectable> {
    private final InjectedValue<NamingStore> namingStoreValue = new InjectedValue<NamingStore>();
    private final String name;
    private final InjectedValue<JndiInjectable> jndiInjectableInjector = new InjectedValue<JndiInjectable>();

    /**
     * Construct new instance.
     *
     * @param name  The JNDI name to use for binding
     */
    public BinderService(final String name) {
        this.name = name;
    }

    /**
     * Bind the entry into the injected context.
     *
     * @param context The start context
     * @throws StartException If the entity can not be bound
     */
    public synchronized void start(StartContext context) throws StartException {
        final NamingStore namingStore = namingStoreValue.getValue();
        try {
            final Reference reference = JndiInjectableObjectFactory.createReference(context.getController().getName());
            final Name name = NameParser.INSTANCE.parse(this.name);
            namingStore.bindCreatingParents(null, name, reference, Reference.class.getName());
        } catch (NamingException e) {
            throw new StartException("Failed to bind resource into naming store [" + namingStore + "] at location [" + name + "]", e);
        }
    }

    /**
     * Unbind the entry from the injected context.
     *
     * @param context The stop context
     */
    public synchronized void stop(StopContext context) {
        final NamingStore namingStore = namingStoreValue.getValue();
        try {
            namingStore.unbind(null, NameParser.INSTANCE.parse(name));
        } catch (NamingException e) {
            throw new IllegalStateException("Failed to unbind resource from naming store [" + namingStore + "] at location [" + name + "]", e);
        }
    }

    /**
     * Get the value from the injected context.
     *
     * @return The value of the named entry
     * @throws IllegalStateException
     */
    @SuppressWarnings("unchecked")
    public synchronized JndiInjectable getValue() throws IllegalStateException {
        return jndiInjectableInjector.getValue();
    }

    /**
     * Get the injector for the item to be bound.
     *
     * @return the injector
     */
    public Injector<JndiInjectable> getJndiInjectableInjector() {
        return jndiInjectableInjector;
    }

    /**
     * Get the naming store injector.
     *
     * @return the injector
     */
    public Injector<NamingStore> getNamingStoreInjector() {
        return namingStoreValue;
    }
}
