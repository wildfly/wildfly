/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.service;

import java.util.Map;

import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.service.ServiceDependency;
import org.wildfly.service.ServiceInstaller;

/**
 * {@link ServiceActivator} extension that automates singleton service installation.
 * @author Paul Ferraro
 */
public interface SingletonServiceActivator extends ServiceActivator {

    @Override
    default void activate(ServiceActivatorContext context) {
        Map.Entry<String, String[]> resolved = ServiceTargetFactory.SERVICE_DESCRIPTOR.resolve(this.getSingletonPolicy());
        ServiceName baseName = ServiceName.parse(resolved.getKey());
        ServiceName name = (resolved.getValue().length > 0) ? baseName.append(resolved.getValue()) : baseName;
        ServiceDependency<SingletonServiceTargetFactory> singletonTargetFactory = ServiceDependency.on(name);
        ServiceInstaller installer = new ServiceInstaller() {
            @Override
            public ServiceController<?> install(ServiceTarget target) {
                SingletonServiceTarget singletonTarget = singletonTargetFactory.get().createSingletonServiceTarget(target);
                SingletonServiceActivator.this.activate(new SingletonServiceActivatorContext() {
                    @Override
                    public ServiceRegistry getServiceRegistry() {
                        return context.getServiceRegistry();
                    }

                    @Override
                    public SingletonServiceTarget getServiceTarget() {
                        return singletonTarget;
                    }
                });
                return null;
            }
        };
        ServiceInstaller.builder(installer).requires(singletonTargetFactory).build().install(context.getServiceTarget());
    }

    /**
     * Returns the singleton policy used for service installation, or null, if the default policy is to be used.
     * @return a singleton policy name, or null
     */
    default String getSingletonPolicy() {
        return null;
    }

    /**
     * Activates singleton services.
     * @param context a context that exposes a target for singleton service installation
     */
    void activate(SingletonServiceActivatorContext context);
}
