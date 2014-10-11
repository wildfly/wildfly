/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jmx.model;

import org.jboss.as.controller.ModelControllerServiceInitialization;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Provides integration between the JMX layer and the core management layer beyond what is possible
 * via the Extension interface.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class ManagementModelIntegration implements ModelControllerServiceInitialization {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("jmx", "management", "integration");

    @Override
    public void initializeStandalone(ServiceTarget target, ManagementResourceRegistration registration, Resource resource) {
        ManagementModelProvider provider =
                new ManagementModelProvider(new ResourceAndRegistration(resource, registration));
        target.addService(SERVICE_NAME, provider).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
    }

    @Override
    public void initializeDomain(ServiceTarget target, ManagementResourceRegistration registration, Resource resource) {
        // not relevant to domain model;
    }

    @Override
    public void initializeHost(ServiceTarget target, ManagementResourceRegistration registration, Resource resource) {
        // not relevant to host controller;
    }

    static final class ResourceAndRegistration {
        private final Resource resource;
        private final ImmutableManagementResourceRegistration registry;

        private ResourceAndRegistration(final Resource resource, ImmutableManagementResourceRegistration registry) {
            this.resource = resource;
            this.registry = registry;
        }

        Resource getResource() {
            return resource;
        }

        ImmutableManagementResourceRegistration getRegistration() {
            return registry;
        }
    }

    public final class ManagementModelProvider implements Service<ManagementModelProvider> {
        private final ResourceAndRegistration resourceAndRegistration;

        private ManagementModelProvider(ResourceAndRegistration resourceAndRegistration) {
            this.resourceAndRegistration = resourceAndRegistration;
        }

        @Override
        public void start(StartContext startContext) throws StartException {
            // no-op
        }

        @Override
        public void stop(StopContext stopContext) {
            // no-op;
        }

        @Override
        public ManagementModelProvider getValue() throws IllegalStateException, IllegalArgumentException {
            return this;
        }

        // CRITICAL -- cannot be made protected or public!
        ResourceAndRegistration getResourceAndRegistration() {
            return resourceAndRegistration;
        }
    }
}
