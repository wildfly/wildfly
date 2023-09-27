/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
