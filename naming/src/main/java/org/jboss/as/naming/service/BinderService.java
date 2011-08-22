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

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for binding and unbinding a entry into a naming context.  This service can be used as a dependency for
 * any service that needs to retrieve this entry from the context.
 *
 * @author John E. Bailey
 */
public class BinderService implements Service<ManagedReferenceFactory> {

    private static final Logger logger = Logger.getLogger(BinderService.class);

    private final InjectedValue<ServiceBasedNamingStore> namingStoreValue = new InjectedValue<ServiceBasedNamingStore>();
    private final String name;
    private final InjectedValue<ManagedReferenceFactory> managedReferenceFactory = new InjectedValue<ManagedReferenceFactory>();
    private final Object source;
    private short refcnt = 0;
    private ServiceController<?> controller;

    /**
     * Construct new instance.
     *
     * @param name The JNDI name to use for binding. May be either an absolute or relative name
     */
    public BinderService(final String name, Object source) {
        if (name.startsWith("java:")) {
            //this is an absolute reference
            this.name = name.substring(name.indexOf('/') + 1);
        } else {
            this.name = name;
        }
        this.source = source;
    }

    public BinderService(final String name) {
        this(name, null);
    }

    public Object getSource() {
        return source;
    }

    public synchronized void acquire() {
        refcnt++;
    }

    public synchronized void release() {
        if (--refcnt <= 0)
            controller.setMode(ServiceController.Mode.REMOVE);
    }

    /**
     * Bind the entry into the injected context.
     *
     * @param context The start context
     * @throws StartException If the entity can not be bound
     */
    public synchronized void start(StartContext context) throws StartException {
        final ServiceBasedNamingStore namingStore = namingStoreValue.getValue();
        ServiceController<?> controller = context.getController();
        this.controller = controller;
        namingStore.add(controller.getName());
        logger.tracef("Bound resource %s into naming store %s", name, namingStore);
    }

    /**
     * Unbind the entry from the injected context.
     *
     * @param context The stop context
     */
    public synchronized void stop(StopContext context) {
        final ServiceBasedNamingStore namingStore = namingStoreValue.getValue();
        namingStore.remove(context.getController().getName());
    }

    /**
     * Get the value from the injected context.
     *
     * @return The value of the named entry
     * @throws IllegalStateException
     */
    @SuppressWarnings("unchecked")
    public synchronized ManagedReferenceFactory getValue() throws IllegalStateException {
        return managedReferenceFactory.getValue();
    }

    /**
     * Get the injector for the item to be bound.
     *
     * @return the injector
     */
    public Injector<ManagedReferenceFactory> getManagedObjectInjector() {
        return managedReferenceFactory;
    }

    /**
     * Get the naming store injector.
     *
     * @return the injector
     */
    public Injector<ServiceBasedNamingStore> getNamingStoreInjector() {
        return namingStoreValue;
    }
}
