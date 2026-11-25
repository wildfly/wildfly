/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.wildfly.clustering.function.Function;
import org.wildfly.subsystem.resource.ChildResourceProvider;
import org.wildfly.subsystem.resource.DynamicResource;

/**
 * Provides component child resources for the runtime resource of a cache.
 * @author Paul Ferraro
 */
public class CacheRuntimeResourceProvider implements ChildResourceProvider {

    private static final Map<String, Resource> COMPONENTS = EnumSet.of(ComponentResourceRegistration.LOCKING, ComponentResourceRegistration.PARTITION_HANDLING, ComponentResourceRegistration.PERSISTENCE, ComponentResourceRegistration.TRANSACTION)
            .stream()
            .map(ResourceRegistration::getPathElement)
            .collect(Collectors.toMap(PathElement::getValue, Function.of(PlaceholderResource.INSTANCE)));

    private static final ChildResourceProvider COMPONENT_PROVIDER = new ChildResourceProvider() {
        @Override
        public Resource getChild(String name) {
            return COMPONENTS.get(name);
        }

        @Override
        public Set<String> getChildren() {
            return COMPONENTS.keySet();
        }
    };
    private static final Resource COMPONENT_RESOURCE = new DynamicResource(PlaceholderResource.INSTANCE, Map.of(ComponentResourceRegistration.WILDCARD.getPathElement().getKey(), COMPONENT_PROVIDER));

    private final Set<String> children = ConcurrentHashMap.newKeySet();

    @Override
    public Resource getChild(String name) {
        return this.children.contains(name) ? COMPONENT_RESOURCE : null;
    }

    @Override
    public Set<String> getChildren() {
        return this.children;
    }
}
