/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.Builder;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Defines a singleton policy.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link org.wildfly.clustering.singleton.service.SingletonPolicy}.
 */
@Deprecated(forRemoval = true)
public interface SingletonPolicy extends org.wildfly.clustering.singleton.service.SingletonPolicy {
    NullaryServiceDescriptor<SingletonPolicy> DEFAULT_SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.clustering.singleton.default-policy", SingletonPolicy.class);
    UnaryServiceDescriptor<SingletonPolicy> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.singleton.policy", DEFAULT_SERVICE_DESCRIPTOR);

    /**
     * Creates a singleton service builder.
     * @param name the name of the service
     * @param service the service to run when elected as the primary node
     * @return a builder
     * @deprecated Use {@link #createSingletonServiceConfigurator(ServiceName)} instead.
     */
    @Deprecated
    <T> Builder<T> createSingletonServiceBuilder(ServiceName name, Service<T> service);

    /**
     * Creates a singleton service builder.
     * @param name the name of the service
     * @param primaryService the service to run when elected as the primary node
     * @param backupService the service to run when not elected as the primary node
     * @return a builder
     * @deprecated Use {@link #createSingletonServiceConfigurator(ServiceName)} instead.
     */
    @Deprecated
    <T> Builder<T> createSingletonServiceBuilder(ServiceName name, Service<T> primaryService, Service<T> backupService);
}
