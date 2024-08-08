/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.service;

import org.jboss.msc.service.ServiceTarget;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Factory for creating service targets for singleton service installation.
 * @author Paul Ferraro
 */
public interface ServiceTargetFactory {
    NullaryServiceDescriptor<ServiceTargetFactory> DEFAULT_SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.clustering.singleton.default-service-target-factory", ServiceTargetFactory.class);
    UnaryServiceDescriptor<ServiceTargetFactory> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.singleton.service-target-factory", DEFAULT_SERVICE_DESCRIPTOR);

    /**
     * Creates a service target for singleton service installation from the specified MSC service target.
     * @param target a service target for singleton services installation.
     * @return
     */
    ServiceTarget createSingletonServiceTarget(ServiceTarget target);
}
