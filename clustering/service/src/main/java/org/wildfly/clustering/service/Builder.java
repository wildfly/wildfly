/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.service;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;

/**
 * Encapsulates the logic for building a service.
 * @author Paul Ferraro
 * @param <T> the type of value provided by services built by this builder
 * @deprecated Replaced by {@link ServiceConfigurator}.
 */
@Deprecated(forRemoval = true)
public interface Builder<T> extends ServiceConfigurator {
    /**
     * Builds a service into the specified target.
     * @param target the service installation target
     * @return a service builder
     */
    @Override
    ServiceBuilder<T> build(ServiceTarget target);
}
