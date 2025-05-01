/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;

/**
 * Factory methods for creating {@link BinaryServiceDescriptor} instances for cache components.
 * @author Paul Ferraro
 */
public class BinaryServiceDescriptorFactory {

    static <T> BinaryServiceDescriptor<T> createServiceDescriptor(ResourceRegistration registration, Class<T> type) {
        return createServiceDescriptor(List.of(registration), type);
    }

    static <T> BinaryServiceDescriptor<T> createServiceDescriptor(List<ResourceRegistration> registrations, Class<T> type) {
        Stream.Builder<String> builder = Stream.<String>builder().add(InfinispanServiceDescriptor.CACHE_CONFIGURATION.getName());
        for (ResourceRegistration registration : registrations) {
            PathElement path = registration.getPathElement();
            builder.accept(path.getKey());
            if (!path.isWildcard()) {
                builder.accept(path.getValue());
            }
        }
        return BinaryServiceDescriptor.of(builder.build().collect(Collectors.joining(".")), type);
    }

    private BinaryServiceDescriptorFactory() {
        // Hide
    }
}
