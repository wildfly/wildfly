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

import static org.jboss.as.naming.NamingLogger.ROOT_LOGGER;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.JndiNamingDependencyProcessor;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for binding and unbinding a entry into a naming context.  This service can be used as a dependency for
 * any service that needs to retrieve this entry from the context.
 *
 * @author John E. Bailey
 * @author Eduardo Martins
 */
public class BinderService implements Service<ManagedReferenceFactory> {

    private final InjectedValue<ServiceBasedNamingStore> namingStoreValue = new InjectedValue<ServiceBasedNamingStore>();
    private final String name;
    private final InjectedValue<ManagedReferenceFactory> managedReferenceFactory = new InjectedValue<ManagedReferenceFactory>();
    private final Object source;
    private final AtomicInteger refcnt = new AtomicInteger(0);
    private ServiceController<?> controller;
    private final ServiceName deploymentServiceName;

    /**
     * Construct new instance.
     *
     * @param name The JNDI name to use for binding. May be either an absolute or relative name
     * @param source
     * @param deploymentServiceName the service name for the related deployment unit
     */
    public BinderService(final String name, Object source, ServiceName deploymentServiceName) {
        if (name.startsWith("java:")) {
            //this is an absolute reference
            this.name = name.substring(name.indexOf('/') + 1);
        } else {
            this.name = name;
        }
        this.source = source;
        this.deploymentServiceName = deploymentServiceName;
    }

    /**
     * Construct new instance.
     *
     * @param name The JNDI name to use for binding. May be either an absolute or relative name
     */
    public BinderService(final String name, Object source) {
        this(name,source,null);
    }

    public BinderService(final String name) {
        this(name, null, null);
    }

    public Object getSource() {
        return source;
    }

    public void acquire() {
        refcnt.incrementAndGet();
    }

    public void release() {
        if (refcnt.decrementAndGet() <= 0)
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
        ROOT_LOGGER.tracef("Bound resource %s into naming store %s (service name %s)", name, namingStore, controller.getName());
        if(deploymentServiceName != null) {
            // add this controller service name to the related deployment runtime bindings management service, which if stop will release this service too, thus removing the bind
            final Set<ServiceName> duBindingReferences = (Set<ServiceName>) controller.getServiceContainer().getService(JndiNamingDependencyProcessor.serviceName(deploymentServiceName)).getValue();
            duBindingReferences.add(controller.getName());
        }
    }

    /**
     * Unbind the entry from the injected context.
     *
     * @param context The stop context
     */
    public synchronized void stop(StopContext context) {
        final ServiceBasedNamingStore namingStore = namingStoreValue.getValue();
        namingStore.remove(context.getController().getName());
        if(deploymentServiceName != null) {
            // remove the service name from the related deployment runtime bindings management service,
            final Set<ServiceName> duBindingReferences = (Set<ServiceName>) controller.getServiceContainer().getService(JndiNamingDependencyProcessor.serviceName(deploymentServiceName)).getValue();
            if(duBindingReferences != null) {
                // the set is null if the binder service was stopped by the deployment unit undeploy
                duBindingReferences.remove(controller.getName());
            }
        }
    }

    /**
     * Get the value from the injected context.
     *
     * @return The value of the named entry
     * @throws IllegalStateException
     */
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

    @Override
    public String toString() {
        return "BinderService[name=" + name + ",source=" + source + ",deployment=" + deploymentServiceName +"]";
    }
}
