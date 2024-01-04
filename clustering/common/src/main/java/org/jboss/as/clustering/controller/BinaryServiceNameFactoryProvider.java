/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;


import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;

/**
 * Provides a factory for generating a {@link ServiceName} for a binary requirement.
 * @author Paul Ferraro
 */
public interface BinaryServiceNameFactoryProvider extends BinaryServiceNameFactory {

    BinaryServiceNameFactory getServiceNameFactory();

    @Override
    default ServiceName getServiceName(OperationContext context, String parent, String child) {
        return this.getServiceNameFactory().getServiceName(context, parent, child);
    }

    @Override
    default ServiceName getServiceName(CapabilityServiceSupport support, String parent, String child) {
        return this.getServiceNameFactory().getServiceName(support, parent, child);
    }
}
