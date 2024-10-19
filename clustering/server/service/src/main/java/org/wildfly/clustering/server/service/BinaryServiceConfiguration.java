/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.service;

import java.util.AbstractMap;
import java.util.Map;

import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.msc.service.ServiceName;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Encapsulates the configuration of a service described by a tuple.
 * @author Paul Ferraro
 */
public interface BinaryServiceConfiguration {
    default Map.Entry<String, String> getName() {
        return new AbstractMap.SimpleImmutableEntry<>(this.getParentName(), this.getChildName());
    }

    String getParentName();
    String getChildName();

    default BinaryServiceConfiguration withChildName(String childName) {
        return BinaryServiceConfiguration.of(this.getParentName(), childName);
    }

    default <T> ServiceDependency<T> getServiceDependency(BinaryServiceDescriptor<T> descriptor) {
        return ServiceDependency.on(descriptor, this.getParentName(), this.getChildName());
    }

    default <T> ServiceDependency<T> getServiceDependency(UnaryServiceDescriptor<T> descriptor) {
        return ServiceDependency.on(descriptor, this.getParentName());
    }

    default ServiceName resolveServiceName(BinaryServiceDescriptor<?> descriptor) {
        return ServiceNameFactory.resolveServiceName(descriptor, this.getParentName(), this.getChildName());
    }

    default ServiceName resolveServiceName(UnaryServiceDescriptor<?> descriptor) {
        return ServiceNameFactory.resolveServiceName(descriptor, this.getParentName());
    }

    static BinaryServiceConfiguration of(String parentName, String childName) {
        return new BinaryServiceConfiguration() {
            @Override
            public String getParentName() {
                return parentName;
            }

            @Override
            public String getChildName() {
                return childName;
            }
        };
    }
}
