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

import org.jboss.as.ee.component.AbstractComponentDescription;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.server.ManagedReference;
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

    public ManagedBeanComponentInstantiator(DeploymentUnit deploymentUnit, AbstractComponentDescription componentDescription) {
        final ServiceName baseName = deploymentUnit.getServiceName().append("component").append(componentDescription.getComponentName());
        serviceRegistry = deploymentUnit.getServiceRegistry();
        serviceNames.add(baseName.append("START"));
        if(componentDescription.getViewClassNames().size() != 1) {
            throw new RuntimeException("Servlet components must have exactly one view: " + componentDescription.getComponentName());
        }
        String name = componentDescription.getViewClassNames().iterator().next();
        serviceName = baseName.append("VIEW").append(name);
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
        return viewServiceServiceController.getValue().getReference();
    }

    @Override
    public Set<ServiceName> getServiceNames() {
        return serviceNames;
    }


}
