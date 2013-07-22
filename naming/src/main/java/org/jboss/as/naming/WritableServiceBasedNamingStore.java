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
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.value.ImmediateValue;

/**
 * @author John Bailey
 * @author Eduardo Martins
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class WritableServiceBasedNamingStore extends ServiceBasedNamingStore implements WritableNamingStore {
    private static final ThreadLocalStack<ServiceName> WRITE_OWNER = new ThreadLocalStack<ServiceName>();

    private final ServiceTarget serviceTarget;

    public WritableServiceBasedNamingStore(ServiceRegistry serviceRegistry, ServiceName serviceNameBase, ServiceTarget serviceTarget) {
        super(serviceRegistry, serviceNameBase);
        this.serviceTarget = serviceTarget;
    }

    public void bind(final Name name, final Object object) throws NamingException {
        final ServiceName deploymentUnitServiceName = requireOwner();
        final ServiceName bindName = buildServiceName(name);
        bind(name, bindName, object, deploymentUnitServiceName);
    }

    @SuppressWarnings("unchecked")
    private void bind(final Name name, final ServiceName bindName, final Object object, final ServiceName deploymentUnitServiceName) throws NamingException {
        try {
            final BinderService binderService = new BinderService(name.toString(), null, deploymentUnitServiceName);
            final ServiceBuilder<?> builder = serviceTarget.addService(bindName, binderService)
                    .addDependency(getServiceNameBase(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                    .addInjection(binderService.getManagedObjectInjector(), new ValueManagedReferenceFactory(new ImmediateValue<Object>(object)))
                    .setInitialMode(ServiceController.Mode.ACTIVE);
            final ServiceController<?> binderServiceController = builder.install();
            final StabilityMonitor monitor = new StabilityMonitor();
            monitor.addController(binderServiceController);
            try {
                monitor.awaitStability();
            } finally {
                monitor.removeController(binderServiceController);
            }
            final Exception startException = binderServiceController.getStartException();
            if (startException != null) {
                throw startException;
            }
            binderService.acquire();
        } catch (Exception e) {
            throw namingException("Failed to bind [" + object + "] at location [" + bindName + "]", e);
        }
    }

    public void bind(final Name name, final Object object, final Class<?> bindType) throws NamingException {
        bind(name, object);
    }

    public void rebind(Name name, Object object) throws NamingException {
        final ServiceName deploymentUnitServiceName = requireOwner();
        final ServiceName bindName = buildServiceName(name);
        try {
            unbind(name, bindName);
        } catch (NamingException ignore) {
            // rebind may fail if there is no existing binding
        }
        bind(name, bindName, object, deploymentUnitServiceName);
    }

    public void rebind(final Name name, final Object object, final Class<?> bindType) throws NamingException {
        rebind(name, object);
    }

    @SuppressWarnings("unchecked")
    public void unbind(final Name name) throws NamingException {
        requireOwner();
        final ServiceName bindName = buildServiceName(name);
        // do the unbinding
        unbind(name, bindName);
    }

    private void unbind(final Name name, final ServiceName bindName) throws NamingException {
        final ServiceController<?> controller = getServiceRegistry().getService(bindName);
        if (controller == null) {
            throw MESSAGES.cannotResolveService(bindName);
        }
        controller.setMode(ServiceController.Mode.REMOVE);
        final StabilityMonitor monitor = new StabilityMonitor();
        monitor.addController(controller);
        try {
            monitor.awaitStability();
        } catch (Exception e) {
            throw namingException("Failed to unbind [" + bindName + "]", e);
        } finally {
            monitor.removeController(controller);
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

    public static void pushOwner(final ServiceName du) {
        WRITE_OWNER.push(du);
    }

    public static void popOwner() {
        WRITE_OWNER.pop();
    }

}
