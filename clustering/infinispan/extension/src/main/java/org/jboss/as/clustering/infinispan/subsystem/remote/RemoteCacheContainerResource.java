/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.wildfly.clustering.server.Registrar;
import org.wildfly.clustering.server.Registration;
import org.wildfly.subsystem.resource.ChildResourceProvider;
import org.wildfly.subsystem.resource.DynamicResource;

/**
 * A dynamic resource for a remote cache container.
 * @author Paul Ferraro
 */
public class RemoteCacheContainerResource extends DynamicResource implements Registrar<String> {

    private static final String CHILD_TYPE = RemoteCacheRuntimeResourceDefinitionRegistrar.REGISTRATION.getPathElement().getKey();

    public RemoteCacheContainerResource(Resource resource) {
        this(resource, Collections.singletonMap(CHILD_TYPE, new ChildResourceProvider() {
            private final Set<String> caches = ConcurrentHashMap.newKeySet();

            @Override
            public Resource getChild(String name) {
                return this.caches.contains(name) ? PlaceholderResource.INSTANCE : null;
            }

            @Override
            public Set<String> getChildren() {
                return this.caches;
            }
        }));
    }

    private RemoteCacheContainerResource(Resource resource, Map<String, ChildResourceProvider> providers) {
        super(resource, providers, RemoteCacheContainerResource::new);
    }

    @Override
    public Registration register(String cache) {
        ChildResourceProvider handler = this.apply(CHILD_TYPE);
        handler.getChildren().add(cache);
        return new Registration() {
            @Override
            public void close() {
                handler.getChildren().remove(cache);
            }
        };
    }
}
