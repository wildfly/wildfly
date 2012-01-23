/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.web.deployment.component;

import static org.jboss.as.web.WebMessages.MESSAGES;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

import java.util.HashSet;
import java.util.Set;

/**
 * Instantiator for web components that are annotated with the ManagedBean annotation
 *
 * @author Stuart Douglas
 */
public class ManagedBeanComponentInstantiator implements ComponentInstantiator {

    private volatile ServiceController<ComponentView> viewServiceServiceController;
    private final Set<ServiceName> serviceNames = new HashSet<ServiceName>();
    private final ServiceName serviceName;
    private final ServiceRegistry serviceRegistry;

    public ManagedBeanComponentInstantiator(DeploymentUnit deploymentUnit, ComponentDescription componentDescription) {
        final ServiceName baseName = deploymentUnit.getServiceName().append("component").append(componentDescription.getComponentName());
        serviceRegistry = deploymentUnit.getServiceRegistry();
        serviceNames.add(baseName.append("START"));
        if(componentDescription.getViews() == null || componentDescription.getViews().size() != 1) {
            throw MESSAGES.servletsMustHaveOneView(componentDescription.getComponentName());
        }
        ViewDescription view = componentDescription.getViews().iterator().next();
        String viewClassName = view.getViewClassName();
        serviceName = baseName.append("VIEW").append(viewClassName);
        serviceNames.add(serviceName);

    }

    @Override
    public ManagedReference getReference() {
        if(viewServiceServiceController == null) {
            synchronized (this) {
                if(viewServiceServiceController == null) {
                    viewServiceServiceController = (ServiceController<ComponentView>) serviceRegistry.getRequiredService(serviceName);
                }

            }
        }
        throw new IllegalStateException(MESSAGES.notImplemented());
        //return viewServiceServiceController.getValue().getReference();
    }

    @Override
    public Set<ServiceName> getServiceNames() {
        return serviceNames;
    }

    @Override
    public ManagedReference initializeInstance(final Object instance) {
        throw new UnsupportedOperationException(MESSAGES.notImplemented());
    }


}
