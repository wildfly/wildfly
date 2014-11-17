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

import org.jboss.as.naming.deployment.JndiNamingDependencyProcessor;
import org.jboss.as.naming.deployment.RuntimeBindReleaseService;
import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.naming.util.ThreadLocalStack;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.value.ImmediateValue;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import java.util.Hashtable;

import static org.jboss.as.naming.util.NamingUtils.isLastComponentEmpty;
import static org.jboss.as.naming.util.NamingUtils.namingException;

/**
 * @author John Bailey
 * @author Eduardo Martins
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class WritableServiceBasedNamingStore extends ServiceBasedNamingStore implements WritableNamingStore {
    private static final ThreadLocalStack WRITE_OWNER = new ThreadLocalStack();

    private final ServiceTarget serviceTarget;

    public WritableServiceBasedNamingStore(ServiceRegistry serviceRegistry, ServiceName serviceNameBase, ServiceTarget serviceTarget) {
        super(serviceRegistry, serviceNameBase);
        this.serviceTarget = serviceTarget;
    }

    public void bind(final Name name, final Object object, final Class<?> bindType) throws NamingException {
        bind(name, object);
    }

    public void bind(final Name name, final Object object) throws NamingException {
        final Object owner = requireOwner();
        final ServiceName bindName = buildServiceName(name);
        bind(name, object, owner, bindName);
    }

    private void bind(Name name, Object object, Object owner, ServiceName bindName) throws NamingException {
        ServiceTarget serviceTarget = this.serviceTarget;
        ServiceName deploymentUnitServiceName = null;
        if (owner instanceof ServiceName) {
            deploymentUnitServiceName = (ServiceName) owner;
        } else {
            serviceTarget = (ServiceTarget) owner;
        }
        try {
            // unlike on deployment processors, we may assume here it's a shareable bind if the owner is a deployment, because deployment unshareable namespaces are readonly stores
            final BinderService binderService = new BinderService(name.toString(), null, deploymentUnitServiceName != null);
            final ServiceBuilder<?> builder = serviceTarget.addService(bindName, binderService)
                    .addDependency(getServiceNameBase(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                    .addInjection(binderService.getManagedObjectInjector(), new ImmediateManagedReferenceFactory(object))
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
            if (deploymentUnitServiceName != null) {
                binderService.acquire();
                final RuntimeBindReleaseService.References duBindingReferences = (RuntimeBindReleaseService.References) binderServiceController.getServiceContainer().getService(JndiNamingDependencyProcessor.serviceName(deploymentUnitServiceName)).getValue();
                duBindingReferences.add(binderService);
            }
        } catch (Exception e) {
            throw namingException("Failed to bind [" + object + "] at location [" + bindName + "]", e);
        }
    }

    public void rebind(Name name, Object object) throws NamingException {
        final Object owner = requireOwner();
        // re-set the existent binder service injected value
        final ServiceName bindName = buildServiceName(name);
        final ServiceController<?> controller = getServiceRegistry().getService(bindName);
        if (controller == null) {
            bind(name, object, owner, bindName);
        } else {
            final BinderService binderService = (BinderService) controller.getService();
            if (owner instanceof ServiceName) {
                final ServiceName deploymentUnitServiceName = (ServiceName) owner;
                binderService.acquire();
                final RuntimeBindReleaseService.References duBindingReferences = (RuntimeBindReleaseService.References) controller.getServiceContainer().getService(JndiNamingDependencyProcessor.serviceName(deploymentUnitServiceName)).getValue();
                duBindingReferences.add(binderService);
            }
            binderService.getManagedObjectInjector().setValue(new ImmediateValue(new ImmediateManagedReferenceFactory(object)));
        }
    }

    public void rebind(final Name name, final Object object, final Class<?> bindType) throws NamingException {
        rebind(name, object);
    }

    public void unbind(final Name name) throws NamingException {
        requireOwner();
        final ServiceName bindName = buildServiceName(name);
        final ServiceController<?> controller = getServiceRegistry().getService(bindName);
        if (controller == null) {
            throw NamingLogger.ROOT_LOGGER.cannotResolveService(bindName);
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
            throw NamingLogger.ROOT_LOGGER.emptyNameNotAllowed();
        }
        return new NamingContext(name, WritableServiceBasedNamingStore.this, new Hashtable<String, Object>());
    }

    private Object requireOwner() {
        final Object owner = WRITE_OWNER.peek();
        if (owner == null) {
            throw NamingLogger.ROOT_LOGGER.readOnlyNamingContext();
        }
        return owner;
    }

    public static void pushOwner(final ServiceName deploymentUnitServiceName) {
        WRITE_OWNER.push(deploymentUnitServiceName);
    }

    public static void pushOwner(final ServiceTarget serviceTarget) {
        WRITE_OWNER.push(serviceTarget);
    }

    public static void popOwner() {
        WRITE_OWNER.pop();
    }

}
