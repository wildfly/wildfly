/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.clustering.controller.ChildResourceProvider;
import org.jboss.as.clustering.controller.ComplexResource;
import org.jboss.as.clustering.controller.SimpleChildResourceProvider;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.Services;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.Registrar;
import org.wildfly.clustering.server.Registration;

/**
 * Custom resource that additionally reports runtime singleton deployments and services.
 * @author Paul Ferraro
 */
public class SingletonPolicyResource extends ComplexResource implements Registrar<ServiceName> {

    public SingletonPolicyResource(Resource resource) {
        this(resource, Map.of(SingletonRuntimeResourceRegistration.DEPLOYMENT.getPathElement().getKey(), new SimpleChildResourceProvider(ConcurrentHashMap.newKeySet()),
                SingletonRuntimeResourceRegistration.SERVICE.getPathElement().getKey(), new SimpleChildResourceProvider(ConcurrentHashMap.newKeySet())));
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
}
