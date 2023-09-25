/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;

/**
 * Provides a factory for generating a {@link ServiceName} for a binary requirement
 * as well as a factory generating a {@link ServiceName} for a unary requirement.
 * @author Paul Ferraro
 */
public interface DefaultableBinaryServiceNameFactoryProvider extends BinaryServiceNameFactoryProvider {

    /**
     * The factory from which to generate a {@link ServiceName} if the requested name is null.
     * @return a factory for generating service names
     */
    UnaryServiceNameFactory getDefaultServiceNameFactory();

    @Override
    default ServiceName getServiceName(OperationContext context, String parent, String child) {
        return (child != null) ? this.getServiceNameFactory().getServiceName(context, parent, child) : this.getDefaultServiceNameFactory().getServiceName(context, parent);
    }

    @Override
    default ServiceName getServiceName(CapabilityServiceSupport support, String parent, String child) {
        return (child != null) ? this.getServiceNameFactory().getServiceName(support, parent, child) : this.getDefaultServiceNameFactory().getServiceName(support, parent);
    }
}
