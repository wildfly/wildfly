/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.clustering.singleton;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.clustering.controller.ChildResourceProvider;
import org.jboss.as.clustering.controller.ComplexResource;
import org.jboss.as.clustering.controller.SimpleChildResourceProvider;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.Services;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;

/**
 * Custom resource that additionally reports runtime singleton deployments and services.
 * @author Paul Ferraro
 */
public class SingletonPolicyResource extends ComplexResource implements Registrar<ServiceName> {

    private static final String DEPLOYMENT_CHILD_TYPE = SingletonDeploymentResourceDefinition.WILDCARD_PATH.getKey();
    private static final String SERVICE_CHILD_TYPE = SingletonServiceResourceDefinition.WILDCARD_PATH.getKey();

    private static Map<String, ChildResourceProvider> createProviders() {
        Map<String, ChildResourceProvider> providers = new HashMap<>();
        providers.put(DEPLOYMENT_CHILD_TYPE, new SimpleChildResourceProvider(ConcurrentHashMap.newKeySet()));
        providers.put(SERVICE_CHILD_TYPE, new SimpleChildResourceProvider(ConcurrentHashMap.newKeySet()));
        return Collections.unmodifiableMap(providers);
    }

    public SingletonPolicyResource(Resource resource) {
        this(resource, createProviders());
    }

    private SingletonPolicyResource(Resource resource, Map<String, ChildResourceProvider> providers) {
        super(resource, providers, SingletonPolicyResource::new);
    }

    @Override
    public Registration register(ServiceName service) {
        boolean deployment = Services.JBOSS_DEPLOYMENT.isParentOf(service);
        ChildResourceProvider provider = this.apply(deployment ? DEPLOYMENT_CHILD_TYPE : SERVICE_CHILD_TYPE);
        String name = (deployment ? SingletonDeploymentResourceDefinition.pathElement(service) : SingletonServiceResourceDefinition.pathElement(service)).getValue();
        provider.getChildren().add(name);
        return new Registration() {
            @Override
            public void close() {
                provider.getChildren().remove(name);
            }
        };
    }
}
