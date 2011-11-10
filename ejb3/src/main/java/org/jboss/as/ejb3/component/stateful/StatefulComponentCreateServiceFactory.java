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

package org.jboss.as.ejb3.component.stateful;

import org.jboss.as.ee.component.BasicComponentCreateService;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ejb3.cache.CacheInfo;
import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.CacheFactoryService;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.as.ejb3.component.EJBComponentCreateServiceFactory;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * User: jpai
 */
public class StatefulComponentCreateServiceFactory extends EJBComponentCreateServiceFactory {
    @Override
    public BasicComponentCreateService constructService(ComponentConfiguration configuration) {
        if (this.ejbJarConfiguration == null) {
            throw MESSAGES.ejbJarConfigNotBeenSet(this,configuration.getComponentName());
        }
        // setup a injection dependency to inject the DefaultAccessTimeoutService in the stateful bean
        // component create service
        configuration.getCreateDependencies().add(new DependencyConfigurator<StatefulSessionComponentCreateService>() {
            @Override
            public void configureDependency(ServiceBuilder<?> serviceBuilder, StatefulSessionComponentCreateService componentCreateService) throws DeploymentUnitProcessingException {
                serviceBuilder.addDependency(DefaultAccessTimeoutService.STATEFUL_SERVICE_NAME, DefaultAccessTimeoutService.class, componentCreateService.getDefaultAccessTimeoutInjector());
            }
        });
        configuration.getCreateDependencies().add(new DependencyConfigurator<StatefulSessionComponentCreateService>() {
            @Override
            public void configureDependency(ServiceBuilder<?> builder, StatefulSessionComponentCreateService service) throws DeploymentUnitProcessingException {
                builder.addDependency(this.getServiceName(service), CacheFactory.class, service.getCacheFactoryInjector());
            }

            private ServiceName getServiceName(StatefulSessionComponentCreateService service) {
                CacheInfo cache = service.getCache();
                if (cache != null) {
                    return CacheFactoryService.getServiceName(cache.getName());
                }
                return (service.getClustering() == null) ? CacheFactoryService.DEFAULT_SFSB_CACHE_SERVICE_NAME : CacheFactoryService.DEFAULT_CLUSTERED_SFSB_CACHE_SERVICE_NAME;
            }
        });
        return new StatefulSessionComponentCreateService(configuration, this.ejbJarConfiguration);
    }
}
