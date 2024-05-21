/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.service;

import org.jboss.msc.service.ServiceName;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Extension of {@link SingletonPolicy} for customizing singleton service behavior.
 * @author Paul Ferraro
 * @deprecated Superseded by {@link SingletonServiceTargetFactory}.
 */
@Deprecated(forRemoval = true)
public interface SingletonServiceConfiguratorFactory extends SingletonPolicy {
    UnaryServiceDescriptor<SingletonServiceConfiguratorFactory> DEFAULT_SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.cache.default-singleton-service-configurator-factory", SingletonServiceConfiguratorFactory.class);
    BinaryServiceDescriptor<SingletonServiceConfiguratorFactory> SERVICE_DESCRIPTOR = BinaryServiceDescriptor.of("org.wildfly.clustering.cache.singleton-service-configurator-factory", DEFAULT_SERVICE_DESCRIPTOR);

    @Override
    SingletonServiceConfigurator createSingletonServiceConfigurator(ServiceName name);
}
