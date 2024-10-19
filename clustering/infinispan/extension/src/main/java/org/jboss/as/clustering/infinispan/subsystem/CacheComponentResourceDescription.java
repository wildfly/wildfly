/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.commons.configuration.Builder;
import org.jboss.as.controller.PathElement;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;

/**
 * Describes a cache component resource.
 * @author Paul Ferraro
 */
public interface CacheComponentResourceDescription<C, B extends Builder<C>> extends ComponentResourceDescription<C, B> {

    static <T> BinaryServiceDescriptor<T> createServiceDescriptor(PathElement path, Class<T> type) {
        return createServiceDescriptor(List.of(path), type);
    }

    static <T> BinaryServiceDescriptor<T> createServiceDescriptor(List<PathElement> paths, Class<T> type) {
        Stream.Builder<String> builder = Stream.<String>builder().add(InfinispanServiceDescriptor.CACHE_CONFIGURATION.getName());
        for (PathElement path : paths) {
            builder.accept(path.getKey());
            if (!path.isWildcard()) {
                builder.accept(path.getValue());
            }
        }
        return BinaryServiceDescriptor.of(builder.build().collect(Collectors.joining(".")), type);
    }

    @Override
    BinaryServiceDescriptor<C> getServiceDescriptor();
}
