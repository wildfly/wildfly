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
import org.jboss.as.naming.service.SharedBinderService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Service} which on stop releases acquired {@link org.jboss.as.naming.service.SharedBinderService}s.
 *
 * @author Eduardo Martins
 *
 */
public class RuntimeBindReleaseService implements Service<RuntimeBindReleaseService.References> {

    private final References references;

    public RuntimeBindReleaseService(List<SharedBinderService.ReleaseHandler> servicesAcquiredOnDeploy) {
        this.references = new References(servicesAcquiredOnDeploy);
    }

    public RuntimeBindReleaseService() {
        this(null);
    }

    @Override
    public References getValue() throws IllegalStateException, IllegalArgumentException {
        return references;
    }

    @Override
    public void start(StartContext context) throws StartException {

    }

    @Override
    public void stop(StopContext context) {
        references.releaseAll();
    }

    public static class References {

        // List instead of Set because binder services use a counter to track its references, and a deployment may have multiple components acquiring same shared bind
        private List<SharedBinderService.ReleaseHandler> services;

        private References(List<SharedBinderService.ReleaseHandler> servicesAcquiredOnDeploy) {
            if (servicesAcquiredOnDeploy != null) {
                for (SharedBinderService.ReleaseHandler service : servicesAcquiredOnDeploy) {
                    add(service);
                }
            }
        }

        public synchronized void add(SharedBinderService.ReleaseHandler service) {
            if (services == null) {
                services = new ArrayList<>();
            }
            services.add(service);
        }

        public synchronized boolean contains(ServiceName serviceName) {
            if (services != null) {
                for (SharedBinderService.ReleaseHandler service : services) {
                    if (serviceName.equals(service.getServiceName())) {
                        return true;
                    }
                }
            }
            return false;
        }

        public synchronized void releaseAll() {
            if (services != null) {
                for (SharedBinderService.ReleaseHandler service : services) {
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
