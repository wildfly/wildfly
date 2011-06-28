/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.NamingStore;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A service that represents a single combination of a jndi name and an injection source
 *
 * @author Jason T. Greene
 */
public class BindingHandleService implements Service<Void> {
    private final InjectedValue<ManagedReferenceFactory> managedReferenceFactory = new InjectedValue<ManagedReferenceFactory>();

    private final BinderService binder;
    private final ServiceName binderName;
    private final String name;
    private final ServiceVerificationHandler serviceVerificationHandler;

    private AcquireOnStart acquireOnStart;
    private ServiceName namingStoreName;

    /**
     * Construct new instance.
     *
     * @param name  The JNDI name to use for binding. May be either an absolute or relative name
     * @param serviceVerificationHandler As the binding services are not child services they must be added to the handler manually to ensure that the server can correctly report deployment completion
     */
    public BindingHandleService(final String name, ServiceName binderName, Object source, ServiceName namingStoreName, final ServiceVerificationHandler serviceVerificationHandler) {
        this.binderName = binderName;
        this.name = name;
        this.serviceVerificationHandler = serviceVerificationHandler;
        this.binder = new BinderService(name, source);
        this.namingStoreName = namingStoreName;
    }

    public synchronized void start(StartContext context) throws StartException {
        acquireOnStart = new AcquireOnStart();
        ServiceController<?> controller = context.getController();
        try {

            ServiceBuilder<ManagedReferenceFactory> serviceBuilder = context.getController().getServiceContainer().addService(
                    binderName, binder);
                serviceBuilder.addDependency(namingStoreName, NamingStore.class, binder.getNamingStoreInjector());
                serviceBuilder.addInjectionValue(binder.getManagedObjectInjector(), managedReferenceFactory);
                serviceBuilder.addListener(acquireOnStart);
                serviceBuilder.addListener(serviceVerificationHandler);
                serviceBuilder.install();
        } catch (RuntimeException e) {
            @SuppressWarnings("unchecked")
            ServiceController<ManagedReferenceFactory> registered = (ServiceController<ManagedReferenceFactory>) controller.getServiceContainer().getService(binderName);
            if (registered == null)
                throw e;

            BinderService service = (BinderService)registered.getService();
            if (!service.getSource().equals(binder.getSource()))
                throw new IllegalArgumentException("Incompatible conflicting binding at " + name + " source: " + binder.getSource());
            registered.addListener(acquireOnStart);
        }
    }

    /**
     * Unbind the entry from the injected context.
     *
     * @param context The stop context
     */
    public synchronized void stop(StopContext context) {
        @SuppressWarnings("unchecked")
        ServiceController<ManagedReferenceFactory> registered = (ServiceController<ManagedReferenceFactory>) context.getController().getServiceContainer().getService(binderName);
        if (registered == null)
            return;

        BinderService service = (BinderService)registered.getService();

        if (!acquireOnStart.abort())
            service.release();
    }

    public Void getValue() throws IllegalStateException {
        return null;
    }

    /**
     * Get the injector for the item to be bound.
     *
     * @return the injector
     */
    public Injector<ManagedReferenceFactory> getManagedObjectInjector() {
        return managedReferenceFactory;
    }

    private static class AcquireOnStart implements ServiceListener<ManagedReferenceFactory> {
        private boolean acquired = false;

        public synchronized void listenerAdded(ServiceController<? extends ManagedReferenceFactory> serviceController) {
            if (!acquired && serviceController.getSubstate() == ServiceController.Substate.UP) {
                ((BinderService)serviceController.getService()).acquire();
                acquired = true;
            }

        }

        public synchronized boolean abort() {
            if (acquired == false) {
                return true;
            }

            acquired = false;
            return false;
        }

        public synchronized void transition(ServiceController<? extends ManagedReferenceFactory> serviceController, ServiceController.Transition transition) {
            if (!acquired && transition == ServiceController.Transition.STARTING_to_UP) {
                ((BinderService)serviceController.getService()).acquire();
                acquired = true;
            }
        }

        public void serviceRemoveRequested(ServiceController<? extends ManagedReferenceFactory> serviceController) {
        }

        public void serviceRemoveRequestCleared(ServiceController<? extends ManagedReferenceFactory> serviceController) {
        }

        public void dependencyFailed(ServiceController<? extends ManagedReferenceFactory> serviceController) {
        }

        public void dependencyFailureCleared(ServiceController<? extends ManagedReferenceFactory> serviceController) {
        }

        public void immediateDependencyUnavailable(ServiceController<? extends ManagedReferenceFactory> serviceController) {
        }

        public void immediateDependencyAvailable(ServiceController<? extends ManagedReferenceFactory> serviceController) {
        }

        public void transitiveDependencyUnavailable(ServiceController<? extends ManagedReferenceFactory> serviceController) {
        }

        public void transitiveDependencyAvailable(ServiceController<? extends ManagedReferenceFactory> serviceController) {
        }
    }
}
