/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;

/**
 * Provides a factory for generating a {@link ServiceName} for a unary requirement
 * as well as a factory generating a {@link ServiceName} for a default requirement.
 * @author Paul Ferraro
 */
public interface DefaultableUnaryServiceNameFactoryProvider extends UnaryServiceNameFactoryProvider {

    /**
     * The factory from which to generate a {@link ServiceName} if the requested name is null.
     * @return a factory for generating service names
     */
    ServiceNameFactory getDefaultServiceNameFactory();

    @Override
    default ServiceName getServiceName(OperationContext context, String name) {
        return (name != null) ? this.getServiceNameFactory().getServiceName(context, name) : this.getDefaultServiceNameFactory().getServiceName(context);
    }

    @Override
    default ServiceName getServiceName(CapabilityServiceSupport support, String name) {
        return (name != null) ? this.getServiceNameFactory().getServiceName(support, name) : this.getDefaultServiceNameFactory().getServiceName(support);
    }
}
