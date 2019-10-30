/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
