/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.service.component;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Instantiator for MBean's EE components
 *
 * @author Eduardo Martins
 */
public class ServiceComponentInstantiator {

    private volatile BasicComponent component;
    private final ServiceRegistry serviceRegistry;
    private final ServiceName componentStartServiceName;

    public ServiceComponentInstantiator(DeploymentUnit deploymentUnit, ComponentDescription componentDescription) {
        componentStartServiceName = componentDescription.getStartServiceName();
        this.serviceRegistry = deploymentUnit.getServiceRegistry();
    }

    public ServiceName getComponentStartServiceName() {
        return componentStartServiceName;
    }

    @SuppressWarnings("unchecked")
    private synchronized void setupComponent() {
        if (component == null) {
            component = ((ServiceController<BasicComponent>) serviceRegistry.getRequiredService(componentStartServiceName))
                    .getValue();
        }
    }

    public ManagedReference initializeInstance(final Object instance) {
        setupComponent();
        return new ManagedReference() {

            private final ComponentInstance componentInstance = component.createInstance(instance);
            private boolean destroyed;

            @Override
            public synchronized void release() {
                if (!destroyed) {
                    componentInstance.destroy();
                    destroyed = true;
                }
            }

            @Override
            public Object getInstance() {
                return componentInstance.getInstance();
            }
        };
    }

}
