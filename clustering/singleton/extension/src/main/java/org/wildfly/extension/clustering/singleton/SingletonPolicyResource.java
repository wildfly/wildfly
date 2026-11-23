/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.Services;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.Registrar;
import org.wildfly.clustering.server.Registration;
import org.wildfly.subsystem.resource.ChildResourceProvider;
import org.wildfly.subsystem.resource.DynamicResource;

/**
 * Custom resource that additionally reports runtime singleton deployments and services.
 * @author Paul Ferraro
 */
public class SingletonPolicyResource extends DynamicResource implements Registrar<ServiceName> {

    public SingletonPolicyResource(Resource resource) {
        this(resource, Map.of(SingletonRuntimeResourceRegistration.DEPLOYMENT.getPathElement().getKey(), new SimpleChildResourceProvider(),
                SingletonRuntimeResourceRegistration.SERVICE.getPathElement().getKey(), new SimpleChildResourceProvider()));
    }

    private SingletonPolicyResource(Resource resource, Map<String, ChildResourceProvider> providers) {
        super(resource, providers, SingletonPolicyResource::new);
    }

    @Override
    public Registration register(ServiceName service) {
        SingletonRuntimeResourceRegistration registration = Services.JBOSS_DEPLOYMENT.isParentOf(service) ? SingletonRuntimeResourceRegistration.DEPLOYMENT : SingletonRuntimeResourceRegistration.SERVICE;
        ChildResourceProvider provider = this.apply(registration.getPathElement().getKey());
        String value = registration.pathElement(service).getValue();
        provider.getChildren().add(value);
        return new Registration() {
            @Override
            public void close() {
                provider.getChildren().remove(value);
            }
        };
    }

    static class SimpleChildResourceProvider implements ChildResourceProvider {
        private final Set<String> children = ConcurrentHashMap.newKeySet();

        @Override
        public Resource getChild(String name) {
            return this.children.contains(name) ? PlaceholderResource.INSTANCE : null;
        }

        @Override
        public Set<String> getChildren() {
            return this.children;
        }
    }
}
