/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProvider;
import org.jboss.as.ejb3.component.stateful.cache.simple.SimpleStatefulSessionBeanCacheFactoryServiceInstallerFactory;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Defines a CacheFactoryBuilder instance which, during deployment, is used to configure, build and install a CacheFactory for the SFSB being deployed.
 * The CacheFactory resource instances defined here produce bean caches which are non distributed and do not have passivation-enabled.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class SimpleStatefulSessionBeanCacheProviderResourceDefinition extends StatefulSessionBeanCacheProviderResourceDefinition {

    public SimpleStatefulSessionBeanCacheProviderResourceDefinition() {
        super(EJB3SubsystemModel.SIMPLE_CACHE_PATH, UnaryOperator.identity());
    }

    @Override
    public ServiceDependency<StatefulSessionBeanCacheProvider> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return ServiceDependency.of(new StatefulSessionBeanCacheProvider() {
            @Override
            public Iterable<ServiceInstaller> getDeploymentServiceInstallers(DeploymentUnit unit, Set<Class<?>> beanClasses) {
                return List.of();
            }

            @Override
            public Iterable<ServiceInstaller> getStatefulBeanCacheFactoryServiceInstallers(DeploymentUnit unit, StatefulComponentDescription description, String componentName) {
                return List.of(new SimpleStatefulSessionBeanCacheFactoryServiceInstallerFactory<>().apply(description));
            }

            @Override
            public boolean supportsPassivation() {
                return false;
            }
        });
    }
}
