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

package org.wildfly.clustering.server.singleton;

import java.io.Serializable;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;

/**
 * Builds a non-clustered {@link SingletonServiceBuilderFactory}.
 * @author Paul Ferraro
 */
public class LocalSingletonServiceBuilderFactoryBuilder<T extends Serializable> extends SingletonServiceBuilderFactoryServiceNameProvider implements Builder<SingletonServiceBuilderFactory>, Value<SingletonServiceBuilderFactory> {

    /**
     * @param containerName
     * @param cacheName
     */
    public LocalSingletonServiceBuilderFactoryBuilder(String containerName, String cacheName) {
        super(containerName, cacheName);
    }

    @Override
    public ServiceBuilder<SingletonServiceBuilderFactory> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), new ValueService<>(this));
    }

    @Override
    public SingletonServiceBuilderFactory getValue() {
        return new LocalSingletonServiceBuilderFactory();
    }
}
