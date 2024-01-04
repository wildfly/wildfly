/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class SingletonServiceConfigurator implements ServiceConfigurator {

    private final ServiceConfigurator configurator;
    private final LifecycleListener listener;

    public SingletonServiceConfigurator(ServiceConfigurator configurator, LifecycleListener listener) {
        this.configurator = configurator;
        this.listener = listener;
    }

    @Override
    public ServiceName getServiceName() {
        return this.configurator.getServiceName();
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        return this.configurator.build(target).addListener(this.listener);
    }
}
