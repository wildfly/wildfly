/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * Encapsulates a {@link Dependency} on a {@link org.jboss.msc.Service}.
 * @author Paul Ferraro
 */
public class ServiceDependency extends SimpleServiceNameProvider implements Dependency {

    public ServiceDependency(ServiceName name) {
        super(name);
    }

    public ServiceDependency(ServiceNameProvider provider) {
        super(provider.getServiceName());
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        builder.requires(this.getServiceName());
        return builder;
    }
}
