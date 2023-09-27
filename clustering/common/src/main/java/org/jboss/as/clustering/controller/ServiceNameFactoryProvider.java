/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;

/**
 * Provides a factory for generating a {@link ServiceName} for a capability.
 * @author Paul Ferraro
 */
public interface ServiceNameFactoryProvider extends ServiceNameFactory {

    ServiceNameFactory getServiceNameFactory();

    @Override
    default ServiceName getServiceName(OperationContext context) {
        return this.getServiceNameFactory().getServiceName(context);
    }

    @Override
    default ServiceName getServiceName(CapabilityServiceSupport support) {
        return this.getServiceNameFactory().getServiceName(support);
    }
}
