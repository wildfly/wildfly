/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanExtension;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public abstract class ComponentResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator {

    static PathElement pathElement(String name) {
        return PathElement.pathElement("component", name);
    }

    static <T> UnaryServiceDescriptor<T> serviceDescriptor(PathElement path, Class<T> type) {
        Stream.Builder<String> builder = Stream.<String>builder().add(HotRodServiceDescriptor.REMOTE_CACHE_CONFIGURATION.getName());
        builder.accept(path.getKey());
        if (!path.isWildcard()) {
            builder.accept(path.getValue());
        }
        return UnaryServiceDescriptor.of(builder.build().collect(Collectors.joining(".")), type);
    }

    ComponentResourceDefinition(PathElement path) {
        super(path, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(path));
    }

    ComponentResourceDefinition(PathElement path, ResourceDescriptionResolver resolver) {
        super(path, resolver);
    }
}
