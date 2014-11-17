/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.naming.deployment;

import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.naming.service.SharedBinderService;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Service} that manages the deployment's references with respect to bindings, which may be shared among deployments.
 *
 * @author Eduardo Martins
 *
 */
public class SharedBindingReferencesService implements Service<SharedBindingReferencesService.References> {

    /**
     * Retrieves the name of service related with the specified deployment.
     * @param deploymentUnitServiceName
     * @return
     */
    public static ServiceName serviceName(final ServiceName deploymentUnitServiceName) {
        return deploymentUnitServiceName.append("sharedBindings");
    }

    /**
     * Installs a deployment's service.
     * @param deploymentUnit
     * @param serviceTarget
     * @return
     * @throws DeploymentUnitProcessingException
     */
    public static ServiceController<References> install(DeploymentUnit deploymentUnit, ServiceTarget serviceTarget) throws DeploymentUnitProcessingException {
        final ServiceName serviceName = serviceName(deploymentUnit.getServiceName());
        final List<SharedBinderService> sharedBinderServices = deploymentUnit.getAttachmentList(org.jboss.as.naming.deployment.Attachments.SHARED_BINDER_SERVICES);
        final SharedBindingReferencesService service = new SharedBindingReferencesService(sharedBinderServices);
        final ServiceBuilder<References> serviceBuilder = serviceTarget.addService(serviceName, service);
        if (sharedBinderServices != null) {
            for (SharedBinderService sharedBinderService : sharedBinderServices) {
                serviceBuilder.addDependency(sharedBinderService.getServiceName());
            }
        }
        serviceBuilder.addDependency(NamingService.SERVICE_NAME);
        return serviceBuilder.install();
    }

    /**
     * the shared binder services owned
     */
    private final References references = new References();

    /**
     * the shared binder services to acquire on start
     */
    private final List<SharedBinderService> servicesToAcquireOnStart;

    public SharedBindingReferencesService(List<SharedBinderService> servicesToAcquireOnStart) {
        this.servicesToAcquireOnStart = servicesToAcquireOnStart;
    }

    public SharedBindingReferencesService() {
        this(null);
    }

    @Override
    public References getValue() throws IllegalStateException, IllegalArgumentException {
        return references;
    }

    @Override
    public void start(StartContext context) throws StartException {
        references.acquireAll(servicesToAcquireOnStart);
    }

    @Override
    public void stop(StopContext context) {
        references.releaseAll();
    }

    /**
     * The shared binds referenced/owned by the deployment.
     */
    public static class References {

        // List instead of Set because binder services use a counter to track its references, and a deployment may have multiple components acquiring same shared bind
        private List<SharedBinderService> services;

        public synchronized void acquire(SharedBinderService service) {
            if (services == null) {
                services = new ArrayList<>();
            }
            service.acquire();
            services.add(service);
        }

        public synchronized void acquireAll(List<SharedBinderService> servicesToAcquire) {
            if (servicesToAcquire != null) {
                for (SharedBinderService service : servicesToAcquire) {
                    acquire(service);
                }
            }
        }

        public synchronized boolean contains(ServiceName serviceName) {
            if (services != null) {
                for (SharedBinderService service : services) {
                    if (serviceName.equals(service.getServiceName())) {
                        return true;
                    }
                }
            }
            return false;
        }

        public synchronized void releaseAll() {
            if (services != null) {
                for (SharedBinderService service : services) {
                    try {
                        service.release();
                    } catch (Throwable e) {
                        NamingLogger.ROOT_LOGGER.failedToReleaseBinderService(e);
                    }
                }
                services = null;
            }
        }

    }
}
