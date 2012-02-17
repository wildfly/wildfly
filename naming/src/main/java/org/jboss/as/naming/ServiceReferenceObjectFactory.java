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
package org.jboss.as.naming;

import javax.naming.RefAddr;
import org.jboss.as.naming.context.ModularReference;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceController.State;


import static org.jboss.as.naming.NamingMessages.MESSAGES;
/**
 * Abstract object factory that allows for the creation of service references. Object factories that subclass
 * {@link ServiceReferenceObjectFactory} can get access to the value of the service described by the reference.
 * <p/>
 * If the factory state is no {@link State#UP} then the factory will block. If the state is {@link State#START_FAILED} or
 * {@link State#REMOVED} (or the state transactions to one of these states while blocking) an exception is thrown.
 *
 * @author Stuart Douglas
 */
public abstract class ServiceReferenceObjectFactory implements ServiceAwareObjectFactory {

    private volatile ServiceRegistry serviceRegistry;

    /**
     * Create a reference to a sub class of {@link ServiceReferenceObjectFactory} that injects the value of the given service.
     */
    public static Reference createReference(final ServiceName service, Class<? extends ServiceReferenceObjectFactory> factory) {
        return ModularReference.create(Context.class, new ServiceNameRefAdr("srof", service), factory);
    }

    @Override
    public void injectServiceRegistry(ServiceRegistry registry) {
        this.serviceRegistry = registry;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        final Reference reference = (Reference) obj;
        final ServiceNameRefAdr nameAdr = (ServiceNameRefAdr) reference.get("srof");
        if (nameAdr == null) {
            throw MESSAGES.invalidContextReference("srof");
        }
        final ServiceName serviceName = (ServiceName)nameAdr.getContent();
        final ServiceController<?> controller;
        try {
            controller = serviceRegistry.getRequiredService(serviceName);
        } catch (ServiceNotFoundException e) {
            throw MESSAGES.cannotResolveService(serviceName);
        }

        ServiceReferenceListener listener = new ServiceReferenceListener();
        controller.addListener(listener);
        synchronized (listener) {
            // if we are interrupted just let the exception propagate for now
            while (!listener.finished) {
                try {
                    listener.wait();
                } catch (InterruptedException e) {
                    throw MESSAGES.threadInterrupt(serviceName);
                }
            }
        }
        switch (listener.getState()) {
            case UP:
                return getObjectInstance(listener.getValue(), obj, name, nameCtx, environment);
            case START_FAILED:
                throw MESSAGES.cannotResolveService(serviceName, getClass().getName(), "START_FAILED");
            case REMOVED:
                throw MESSAGES.cannotResolveService(serviceName, getClass().getName(), "START_FAILED");
        }
        // we should never get here, as the listener should not notify unless the state was one of the above
        throw MESSAGES.cannotResolveServiceBug(serviceName, getClass().getName(), listener.getState().toString());
    }

    /**
     * Handles the service reference. The parameters are the same as
     * {@link ObjectFactory#getObjectInstance(Object, Name, Context, Hashtable)}, but with the addition of the service value as
     * the first parameter.
     */
    public abstract Object getObjectInstance(Object serviceValue, Object obj, Name name, Context nameCtx,
                                             Hashtable<?, ?> environment) throws Exception;

    /**
     * listener that notifies when the service changes state
     */
    @SuppressWarnings("unchecked")
    private class ServiceReferenceListener extends AbstractServiceListener {

        private State state;
        private boolean finished = false;
        private Object value;

        @Override
        public synchronized void listenerAdded(ServiceController controller) {
            handleStateChange(controller);
        }

        @Override
        public synchronized void dependencyFailed(ServiceController controller) {
            handleStateChange(controller);
        }

        @Override
        public synchronized void transition(final ServiceController controller, final ServiceController.Transition transition) {
            switch (transition) {
                case STARTING_to_START_FAILED:
                case STARTING_to_UP:
                case REMOVING_to_REMOVED: {
                    handleStateChange(controller);
                    break;
                }
            }
        }

        private void handleStateChange(ServiceController controller) {
            state = controller.getState();
            if (state == State.UP) {
                value = controller.getValue();
            }
            if (state == State.UP || state == State.START_FAILED || state == State.REMOVED) {
                controller.removeListener(this);
                finished = true;
                notifyAll();
            }
        }

        public State getState() {
            return state;
        }

        public Object getValue() {
            return value;
        }

        public boolean isFinished() {
            return finished;
        }
    }

    private static final class ServiceNameRefAdr extends RefAddr {
        private static final long serialVersionUID = 3677121114687908679L;

        private final ServiceName serviceName;

        private ServiceNameRefAdr(String s, ServiceName serviceName) {
            super(s);
            this.serviceName = serviceName;
        }

        public Object getContent() {
            return serviceName;
        }
    }
}
