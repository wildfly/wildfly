/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.commons.configuration.Builder;
import org.jboss.as.clustering.infinispan.subsystem.ComponentResourceDescription;
import org.jboss.as.controller.PathElement;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Describes a remote cache container component.
 * @author Paul Ferraro
 */
public interface RemoteCacheContainerComponentResourceDescription<C, B extends Builder<C>> extends ComponentResourceDescription<C, B> {

    static <T> UnaryServiceDescriptor<T> createServiceDescriptor(PathElement path, Class<T> type) {
        Stream.Builder<String> builder = Stream.<String>builder().add(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER_CONFIGURATION.getName());
        builder.accept(path.getKey());
        if (!path.isWildcard()) {
            builder.accept(path.getValue());
        }
        return UnaryServiceDescriptor.of(builder.build().collect(Collectors.joining(".")), type);
    }

    @Override
    UnaryServiceDescriptor<C> getServiceDescriptor();
}
