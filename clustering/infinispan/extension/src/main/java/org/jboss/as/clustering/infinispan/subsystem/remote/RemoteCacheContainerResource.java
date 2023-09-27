/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.clustering.controller.ChildResourceProvider;
import org.jboss.as.clustering.controller.ComplexResource;
import org.jboss.as.clustering.controller.SimpleChildResourceProvider;
import org.jboss.as.controller.registry.Resource;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;

/**
 * @author Paul Ferraro
 */
public class RemoteCacheContainerResource extends ComplexResource implements Registrar<String> {

    private static final String CHILD_TYPE = RemoteCacheResourceDefinition.WILDCARD_PATH.getKey();

    public RemoteCacheContainerResource(Resource resource) {
        this(resource, Collections.singletonMap(CHILD_TYPE, new SimpleChildResourceProvider(ConcurrentHashMap.newKeySet())));
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
