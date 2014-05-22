/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.session;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.spi.CacheServiceNames;
import org.wildfly.clustering.web.session.RouteLocator;

/**
 * Builds a {@link RouteLocator} service.
 * @author Paul Ferraro
 */
public class InfinispanRouteLocatorBuilder implements org.wildfly.clustering.web.session.RouteLocatorBuilder {

    @Override
    public ServiceBuilder<RouteLocator> build(ServiceTarget target, ServiceName name, String deploymentName) {
        return InfinispanRouteLocatorService.build(target, name, deploymentName);
    }

    @Override
    public ServiceBuilder<?> buildServerDependency(ServiceTarget target, final Value<? extends Value<String>> route) {
        return target.addService(CacheServiceNames.REGISTRY_ENTRY.getServiceName(InfinispanSessionManagerFactoryBuilder.DEFAULT_CACHE_CONTAINER), new RouteRegistryEntryProviderService(route));
    }
}
