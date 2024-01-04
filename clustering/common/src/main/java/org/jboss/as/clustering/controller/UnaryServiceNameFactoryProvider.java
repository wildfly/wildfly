/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;

/**
 * Provides a factory for generating a {@link ServiceName} for a unary requirement.
 * @author Paul Ferraro
 */
public interface UnaryServiceNameFactoryProvider extends UnaryServiceNameFactory {

    UnaryServiceNameFactory getServiceNameFactory();

    @Override
    default ServiceName getServiceName(OperationContext context, String name) {
        return this.getServiceNameFactory().getServiceName(context, name);
    }

    @Override
    default ServiceName getServiceName(CapabilityServiceSupport support, String name) {
        return this.getServiceNameFactory().getServiceName(support, name);
    }
}
