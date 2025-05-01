/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Map;

import org.jboss.as.controller.registry.Resource;
import org.wildfly.clustering.server.Registrar;
import org.wildfly.clustering.server.Registration;
import org.wildfly.subsystem.resource.ChildResourceProvider;
import org.wildfly.subsystem.resource.DynamicResource;

/**
 * A dynamic resource implementation for a cache container.
 * @author Paul Ferraro
 */
public class CacheContainerResource extends DynamicResource implements Registrar<String> {

    private static final String CHILD_TYPE = CacheRuntimeResourceDefinitionRegistrar.REGISTRATION.getPathElement().getKey();

    public CacheContainerResource(Resource resource) {
        this(resource, Map.of(CHILD_TYPE, new CacheRuntimeResourceProvider()));
    }

    private CacheContainerResource(Resource resource, Map<String, ChildResourceProvider> providers) {
        super(resource, providers, CacheContainerResource::new);
    }

    @Override
    public Registration register(String cache) {
        ChildResourceProvider provider = this.apply(CHILD_TYPE);
        provider.getChildren().add(cache);
        return new Registration() {
            @Override
            public void close() {
                provider.getChildren().remove(cache);
            }
        };
    }
}
