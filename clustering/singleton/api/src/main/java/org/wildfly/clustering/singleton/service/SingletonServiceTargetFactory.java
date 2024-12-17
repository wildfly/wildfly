/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.service;

import org.jboss.msc.service.ServiceTarget;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Factory for creating singleton service targets.
 * @author Paul Ferraro
 */
public interface SingletonServiceTargetFactory extends ServiceTargetFactory {
    UnaryServiceDescriptor<SingletonServiceTargetFactory> DEFAULT_SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.default-singleton-service-target-factory", SingletonServiceTargetFactory.class);
    BinaryServiceDescriptor<SingletonServiceTargetFactory> SERVICE_DESCRIPTOR = BinaryServiceDescriptor.of("org.wildfly.clustering.singleton-service-target-factory", DEFAULT_SERVICE_DESCRIPTOR);

    @Override
    SingletonServiceTarget createSingletonServiceTarget(ServiceTarget target);
}
