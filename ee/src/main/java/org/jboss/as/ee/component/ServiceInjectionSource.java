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

package org.jboss.as.ee.component;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceInjectionSource extends InjectionSource {

    private final ServiceName serviceName;
    private final Class<?> serviceValueType;

    public ServiceInjectionSource(final ServiceName serviceName) {
        this(serviceName, ManagedReferenceFactory.class);
    }

    public ServiceInjectionSource(final ServiceName serviceName, final Class<?> serviceValueType) {
        this.serviceName = serviceName;
        this.serviceValueType = serviceValueType;
    }

    /**
     * {@inheritDoc}
     */
    public void getResourceValue(final ResolutionContext context, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) {
        Injector inject = ManagedReferenceFactory.class.isAssignableFrom(serviceValueType) ? injector : new ManagedReferenceInjector(injector);
        serviceBuilder.addDependency(serviceName, serviceValueType, inject);
    }

    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ServiceInjectionSource == false) {
            return false;
        }
        ServiceInjectionSource obj = (ServiceInjectionSource) other;
        return this.equalTo(obj);
    }

    private boolean equalTo(final ServiceInjectionSource configuration) {
        return configuration != null && serviceName.equals(configuration.serviceName);
    }

    public int hashCode() {
        return serviceName.hashCode();
    }

}
