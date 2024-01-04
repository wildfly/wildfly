/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.singleton;

import org.jboss.as.ee.component.BasicComponentCreateService;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.as.ejb3.component.EJBComponentCreateServiceFactory;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

import java.util.List;

/**
 * User: jpai
 */
public class SingletonComponentCreateServiceFactory extends EJBComponentCreateServiceFactory {

    private final boolean initOnStartup;
    private final List<ServiceName> dependsOn;

    public SingletonComponentCreateServiceFactory(final boolean initServiceOnStartup, final List<ServiceName> dependsOn) {
        this.initOnStartup = initServiceOnStartup;
        this.dependsOn = dependsOn;
    }

    @Override
    public BasicComponentCreateService constructService(ComponentConfiguration configuration) {
        if (this.ejbJarConfiguration == null) {
            throw EjbLogger.ROOT_LOGGER.ejbJarConfigNotBeenSet(this, configuration.getComponentName());
        }
        // setup an injection dependency to inject the DefaultAccessTimeoutService in the singleton bean
        // component create service
        configuration.getCreateDependencies().add(new DependencyConfigurator<SingletonComponentCreateService>() {
            @Override
            public void configureDependency(ServiceBuilder<?> serviceBuilder, SingletonComponentCreateService componentCreateService) throws DeploymentUnitProcessingException {
                serviceBuilder.addDependency(DefaultAccessTimeoutService.SINGLETON_SERVICE_NAME, DefaultAccessTimeoutService.class, componentCreateService.getDefaultAccessTimeoutInjector());
            }
        });
        return new SingletonComponentCreateService(configuration, this.ejbJarConfiguration, this.initOnStartup, dependsOn);
    }
}
