/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful;

import java.util.concurrent.atomic.AtomicLong;

import org.jboss.as.ee.component.BasicComponentCreateService;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentStartService;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.as.ejb3.component.EJBComponentCreateServiceFactory;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.subsystem.DefaultStatefulBeanSessionTimeoutWriteHandler;
import org.jboss.ejb.client.SessionID;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.service.ServiceDependency;

/**
 * User: jpai
 */
public class StatefulComponentCreateServiceFactory extends EJBComponentCreateServiceFactory {
    @Override
    public BasicComponentCreateService constructService(final ComponentConfiguration configuration) {
        if (this.ejbJarConfiguration == null) {
            throw EjbLogger.ROOT_LOGGER.ejbJarConfigNotBeenSet(this, configuration.getComponentName());
        }
        // setup an injection dependency to inject the DefaultAccessTimeoutService and DefaultStatefulSessionTimeoutService
        // in the stateful bean component create service
        configuration.getCreateDependencies().add(new DependencyConfigurator<StatefulSessionComponentCreateService>() {
            @Override
            public void configureDependency(ServiceBuilder<?> serviceBuilder, StatefulSessionComponentCreateService componentCreateService) {
                serviceBuilder.addDependency(DefaultAccessTimeoutService.STATEFUL_SERVICE_NAME, DefaultAccessTimeoutService.class, componentCreateService.getDefaultAccessTimeoutInjector());
                serviceBuilder.addDependency(DefaultStatefulBeanSessionTimeoutWriteHandler.SERVICE_NAME, AtomicLong.class, componentCreateService.getDefaultStatefulSessionTimeoutInjector());
            }
        });
        StatefulComponentDescription description = (StatefulComponentDescription) configuration.getComponentDescription();
        ServiceDependency<StatefulSessionBeanCacheFactory<SessionID, StatefulSessionComponentInstance>> cacheFactory = ServiceDependency.on(description.getCacheFactoryServiceName());
        configuration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
            @Override
            public void configureDependency(ServiceBuilder<?> builder, ComponentStartService service) {
                cacheFactory.accept(builder);
            }
        });
        return new StatefulSessionComponentCreateService(configuration, this.ejbJarConfiguration, cacheFactory);
    }
}
