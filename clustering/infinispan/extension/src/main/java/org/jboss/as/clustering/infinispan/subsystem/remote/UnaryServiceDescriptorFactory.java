/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Factory methods for creating a unary service descriptors.
 * @author Paul Ferraro
 */
public class UnaryServiceDescriptorFactory {

    static <T> UnaryServiceDescriptor<T> createServiceDescriptor(ResourceRegistration registration, Class<T> type) {
        Stream.Builder<String> builder = Stream.<String>builder().add(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER_CONFIGURATION.getName());
        PathElement path = registration.getPathElement();
        builder.accept(path.getKey());
        if (!path.isWildcard()) {
            builder.accept(path.getValue());
        }
        return UnaryServiceDescriptor.of(builder.build().collect(Collectors.joining(".")), type);
    }

    private UnaryServiceDescriptorFactory() {
        // Hide
    }
}
