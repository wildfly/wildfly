/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.commons.configuration.Builder;
import org.jboss.as.controller.PathElement;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * @author Paul Ferraro
 *
 */
public interface CacheContainerComponentResourceDescription<C, B extends Builder<C>> extends ComponentResourceDescription<C, B> {

    static <T> UnaryServiceDescriptor<T> createServiceDescriptor(PathElement path, Class<T> type) {
        Stream.Builder<String> builder = Stream.<String>builder().add(InfinispanServiceDescriptor.CACHE_CONTAINER_CONFIGURATION.getName());
        builder.accept(path.getKey());
        if (!path.isWildcard()) {
            builder.accept(path.getValue());
        }
        return UnaryServiceDescriptor.of(builder.build().collect(Collectors.joining(".")), type);
    }

    @Override
    UnaryServiceDescriptor<C> getServiceDescriptor();
}
