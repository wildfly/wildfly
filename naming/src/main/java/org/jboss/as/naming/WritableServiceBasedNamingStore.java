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

package org.jboss.as.naming;

import static org.jboss.as.naming.NamingMessages.MESSAGES;
import static org.jboss.as.naming.util.NamingUtils.isLastComponentEmpty;
import static org.jboss.as.naming.util.NamingUtils.namingException;

import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;

import org.jboss.as.naming.deployment.JndiNamingDependencyProcessor;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.naming.util.ThreadLocalStack;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.ImmediateValue;

/**
 * @author John Bailey
 * @author Eduardo Martins
 */
public class WritableServiceBasedNamingStore extends ServiceBasedNamingStore implements WritableNamingStore {
    private static final ThreadLocalStack<ServiceName> WRITE_OWNER = new ThreadLocalStack<ServiceName>();

    private final ServiceContainer serviceContainer;

    public WritableServiceBasedNamingStore(ServiceContainer serviceContainer, ServiceName serviceNameBase) {
        super(serviceContainer, serviceNameBase);
        this.serviceContainer = serviceContainer;
    }

    @SuppressWarnings("unchecked")
    public void bind(final Name name, final Object object) throws NamingException {
        final ServiceName deploymentUnitServiceName = requireOwner();
        final ServiceName bindName = buildServiceName(name);
        try {
            final BinderService binderService = new BinderService(name.toString());
            final BindListener listener = new BindListener();
            final ServiceBuilder<?> builder = serviceContainer.addService(bindName, binderService)
                    .addDependency(getServiceNameBase(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                    .addInjection(binderService.getManagedObjectInjector(), new ValueManagedReferenceFactory(new ImmediateValue<Object>(object)))
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .addListener(listener);
            builder.install();
            listener.await();
            binderService.acquire();
            // add the service name to runtime bindings management service, which on stop releases the services.
            final Set<ServiceName> duBindingReferences = (Set<ServiceName>) serviceContainer.getService(JndiNamingDependencyProcessor.serviceName(deploymentUnitServiceName)).getValue();
            duBindingReferences.add(bindName);
        } catch (Exception e) {
            throw namingException("Failed to bind [" + object + "] at location [" + bindName + "]", e);
        }
    }

    public void bind(final Name name, final Object object, final Class<?> bindType) throws NamingException {
        bind(name, object);
    }

    public void rebind(Name name, Object object) throws NamingException {
        try {
            unbind(name);
        } catch (NamingException ignore) {
            // rebind may fail if there is no existing binding
        }

        bind(name, object);
    }

    public void rebind(final Name name, final Object object, final Class<?> bindType) throws NamingException {
        rebind(name, object);
    }

    public void unbind(final Name name) throws NamingException {
        requireOwner();
        final ServiceName bindName = buildServiceName(name);

        final ServiceController<?> controller = getServiceRegistry().getService(bindName);
        if (controller == null) {
            throw MESSAGES.cannotResolveService(bindName);
        }

        final UnbindListener listener = new UnbindListener();
        controller.addListener(listener);
        try {
            listener.await();
        } catch (Exception e) {
            throw namingException("Failed to unbind [" + bindName + "]", e);
        }
    }

    public Context createSubcontext(final Name name) throws NamingException {
        requireOwner();
        if (isLastComponentEmpty(name)) {
            throw MESSAGES.emptyNameNotAllowed();
        }
        return new NamingContext(name, WritableServiceBasedNamingStore.this, new Hashtable<String, Object>());
    }

    private ServiceName requireOwner() {
        final ServiceName owner = WRITE_OWNER.peek();
        if (owner == null) {
            throw MESSAGES.readOnlyNamingContext();
        }
        return owner;
    }

    private static class BindListener extends AbstractServiceListener<Object> {
        private Exception exception;
        private boolean complete;

        public synchronized void transition(ServiceController<? extends Object> serviceController, ServiceController.Transition transition) {
            switch (transition) {
                case STARTING_to_UP: {
                    complete = true;
                    notifyAll();
                    break;
                }
                case STARTING_to_START_FAILED: {
                    complete = true;
                    exception = serviceController.getStartException();
                    notifyAll();
                    break;
                }
                default:
                    break;
            }
        }

        public synchronized void await() throws Exception {
            while(!complete) {
                wait();
            }
            if (exception != null) {
                throw exception;
            }
        }
    }

    private static class UnbindListener extends AbstractServiceListener<Object> {
        private boolean complete;

        public void listenerAdded(ServiceController<?> controller) {
            controller.setMode(ServiceController.Mode.REMOVE);
        }

        public synchronized void transition(ServiceController<? extends Object> serviceController, ServiceController.Transition transition) {
            switch (transition) {
                case REMOVING_to_REMOVED: {
                    complete = true;
                    notifyAll();
                    break;
                }
                default:
                    break;
            }
        }

        public synchronized void await() throws Exception {
            while(!complete) {
                wait();
            }
        }
    }

    public static void pushOwner(final ServiceName du) {
        WRITE_OWNER.push(du);
    }

    public static void popOwner() {
        WRITE_OWNER.pop();
    }

}
