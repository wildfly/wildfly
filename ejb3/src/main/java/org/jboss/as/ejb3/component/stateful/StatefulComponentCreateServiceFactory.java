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
import org.jboss.as.ee.component.ComponentStartService;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.cache.CacheInfo;
import org.jboss.as.ejb3.cache.CacheFactoryBuilder;
import org.jboss.as.ejb3.cache.CacheFactoryBuilderService;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.as.ejb3.component.EJBComponentCreateServiceFactory;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.msc.value.InjectedValue;

/**
 * User: jpai
 */
public class StatefulComponentCreateServiceFactory extends EJBComponentCreateServiceFactory {
    @Override
    public BasicComponentCreateService constructService(final ComponentConfiguration configuration) {
        if (this.ejbJarConfiguration == null) {
            throw EjbLogger.ROOT_LOGGER.ejbJarConfigNotBeenSet(this, configuration.getComponentName());
        }
        // setup an injection dependency to inject the DefaultAccessTimeoutService in the stateful bean
        // component create service
        configuration.getCreateDependencies().add(new DependencyConfigurator<StatefulSessionComponentCreateService>() {
            @Override
            public void configureDependency(ServiceBuilder<?> serviceBuilder, StatefulSessionComponentCreateService componentCreateService) {
                serviceBuilder.addDependency(DefaultAccessTimeoutService.STATEFUL_SERVICE_NAME, DefaultAccessTimeoutService.class, componentCreateService.getDefaultAccessTimeoutInjector());
            }
        });
        configuration.getCreateDependencies().add(new DependencyConfigurator<StatefulSessionComponentCreateService>() {
            @Override
            public void configureDependency(final ServiceBuilder<?> builder, final StatefulSessionComponentCreateService service) {
                builder.addDependency(this.getServiceName(service), CacheFactoryBuilder.class, service.getCacheFactoryBuilderInjector());
            }

            private ServiceName getServiceName(StatefulSessionComponentCreateService service) {
                if (!service.isPassivationCapable()) {
                    return CacheFactoryBuilderService.DEFAULT_PASSIVATION_DISABLED_CACHE_SERVICE_NAME;
                }
                CacheInfo cache = service.getCache();
                return (cache != null) ? CacheFactoryBuilderService.getServiceName(cache.getName()) : CacheFactoryBuilderService.DEFAULT_CACHE_SERVICE_NAME;
            }
        });
        @SuppressWarnings("rawtypes")
        final InjectedValue<CacheFactory> factory = new InjectedValue<>();
        configuration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
            @Override
            public void configureDependency(ServiceBuilder<?> builder, ComponentStartService service) {
                builder.addDependency(configuration.getComponentDescription().getServiceName().append("cache"), CacheFactory.class, factory);
            }
        });
        return new StatefulSessionComponentCreateService(configuration, this.ejbJarConfiguration, factory);
    }
}
