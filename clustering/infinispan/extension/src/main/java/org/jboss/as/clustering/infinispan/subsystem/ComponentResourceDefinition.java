/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public abstract class ComponentResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator {

    static PathElement pathElement(String name) {
        return PathElement.pathElement("component", name);
    }

    static <T> BinaryServiceDescriptor<T> serviceDescriptor(PathElement path, Class<T> type) {
        return serviceDescriptor(List.of(path), type);
    }

    static <T> BinaryServiceDescriptor<T> serviceDescriptor(List<PathElement> paths, Class<T> type) {
        Stream.Builder<String> builder = Stream.<String>builder().add(InfinispanServiceDescriptor.CACHE_CONFIGURATION.getName());
        for (PathElement path : paths) {
            builder.accept(path.getKey());
            if (!path.isWildcard()) {
                builder.accept(path.getValue());
            }
        }
        return BinaryServiceDescriptor.of(builder.build().collect(Collectors.joining(".")), type);
    }

    ComponentResourceDefinition(PathElement path) {
        super(path, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(path));
    }

    ComponentResourceDefinition(PathElement path, ResourceDescriptionResolver resolver) {
        super(path, resolver);
    }
}
