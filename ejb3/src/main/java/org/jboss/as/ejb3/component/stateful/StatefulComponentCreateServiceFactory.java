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

import java.util.concurrent.atomic.AtomicLong;

import org.jboss.as.ee.component.BasicComponentCreateService;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentStartService;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.as.ejb3.component.EJBComponentCreateServiceFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.subsystem.DefaultStatefulBeanSessionTimeoutWriteHandler;
import org.jboss.ejb.client.SessionID;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.SupplierDependency;

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
        SupplierDependency<CacheFactory<SessionID, StatefulSessionComponentInstance>> cacheFactory = new InjectedValueDependency<>(description.getCacheFactoryServiceName(), (Class<CacheFactory<SessionID, StatefulSessionComponentInstance>>) (Class<?>) CacheFactory.class);
        configuration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
            @Override
            public void configureDependency(ServiceBuilder<?> builder, ComponentStartService service) {
                cacheFactory.register(builder);
            }
        });
        return new StatefulSessionComponentCreateService(configuration, this.ejbJarConfiguration, cacheFactory);
    }
}
