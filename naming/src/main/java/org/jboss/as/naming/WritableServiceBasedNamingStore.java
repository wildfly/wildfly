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

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;

import org.jboss.as.naming.service.BinderService;
import org.jboss.as.naming.util.ThreadLocalStack;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;

/**
 * @author John Bailey
 */
public class WritableServiceBasedNamingStore extends ServiceBasedNamingStore implements WritableNamingStore {
    private static final ThreadLocalStack<WriteOwner> WRITE_OWNER = new ThreadLocalStack<WriteOwner>();

    public WritableServiceBasedNamingStore(ServiceRegistry serviceRegistry, ServiceName serviceNameBase) {
        super(serviceRegistry, serviceNameBase);
    }

    public void bind(final Name name, final Object object) throws NamingException {
        final WriteOwner owner = requireOwner();

        final ServiceName bindName = buildServiceName(name);
        final BindListener listener = new BindListener();

        final BinderService binderService = new BinderService(name.toString());
        final ServiceBuilder<?> builder = owner.target.addService(bindName, binderService)
                .addDependency(getServiceNameBase(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .addInjection(binderService.getManagedObjectInjector(), new ValueManagedReferenceFactory(new ImmediateValue<Object>(object)))
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(listener);
        for(ServiceName dependency : owner.dependencies) {
            builder.addDependency(dependency);
        }
        builder.install();
        try {
            listener.await();
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

    private WriteOwner requireOwner() {
        final WriteOwner owner = WRITE_OWNER.peek();
        if (owner == null) {
            throw MESSAGES.readOnlyNamingContext();
        }
        return owner;
    }

    private class BindListener extends AbstractServiceListener<Object> {
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

    private class UnbindListener extends AbstractServiceListener<Object> {
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
            }
        }

        public synchronized void await() throws Exception {
            while(!complete) {
                wait();
            }
        }
    }

    public static void pushOwner(final ServiceTarget target, final ServiceName... dependencies) {
        WRITE_OWNER.push(new WriteOwner(target, dependencies));
    }

    public static void popOwner() {
        WRITE_OWNER.pop();
    }

    private static class WriteOwner {
        private final ServiceTarget target;
        private final ServiceName[] dependencies;

        public WriteOwner(final ServiceTarget target, final ServiceName... dependencies) {
            this.target = target;
            this.dependencies = dependencies;
        }
    }
}
