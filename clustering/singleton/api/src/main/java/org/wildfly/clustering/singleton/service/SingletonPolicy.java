/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.service;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Defines a policy for creating singleton services.
 * @author Paul Ferraro
 */
public interface SingletonPolicy {
    NullaryServiceDescriptor<SingletonPolicy> DEFAULT_SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.clustering.default-singleton-policy", SingletonPolicy.class);
    UnaryServiceDescriptor<SingletonPolicy> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.singleton-policy", DEFAULT_SERVICE_DESCRIPTOR);

    ServiceConfigurator createSingletonServiceConfigurator(ServiceName name);
}
