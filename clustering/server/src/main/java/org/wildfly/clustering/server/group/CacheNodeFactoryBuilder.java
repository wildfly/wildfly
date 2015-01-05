/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.group;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.server.CacheServiceNameProvider;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupServiceName;
import org.wildfly.clustering.spi.GroupServiceName;

/**
 * Builds a {@link InfinispanNodeFactory} service for a cache.
 * @author Paul Ferraro
 */
public class CacheNodeFactoryBuilder extends CacheServiceNameProvider implements Builder<InfinispanNodeFactory>, Value<InfinispanNodeFactory> {

    private final InjectedValue<JGroupsNodeFactory> factory = new InjectedValue<>();

    public CacheNodeFactoryBuilder(String containerName, String cacheName) {
        super(CacheGroupServiceName.NODE_FACTORY, containerName, cacheName);
    }

    @Override
    public ServiceBuilder<InfinispanNodeFactory> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(GroupServiceName.NODE_FACTORY.getServiceName(this.containerName), JGroupsNodeFactory.class, this.factory)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public InfinispanNodeFactory getValue() {
        return new CacheNodeFactory(this.factory.getValue());
    }
}
