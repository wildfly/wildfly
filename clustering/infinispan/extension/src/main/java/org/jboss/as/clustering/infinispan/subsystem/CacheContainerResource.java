/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Collections;
import java.util.Map;

import org.jboss.as.clustering.controller.ChildResourceProvider;
import org.jboss.as.clustering.controller.ComplexResource;
import org.jboss.as.controller.registry.Resource;
import org.wildfly.clustering.server.Registrar;
import org.wildfly.clustering.server.Registration;

/**
 * @author Paul Ferraro
 */
public class CacheContainerResource extends ComplexResource implements Registrar<String> {

    private static final String CHILD_TYPE = CacheRuntimeResourceDefinition.WILDCARD_PATH.getKey();

    public CacheContainerResource(Resource resource) {
        this(resource, Collections.singletonMap(CHILD_TYPE, new CacheRuntimeResourceProvider()));
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
