/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.service;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * Encapsulates a {@link Dependency} on a {@link org.jboss.msc.Service}.
 * @author Paul Ferraro
 */
public class ServiceDependency extends SimpleServiceNameProvider implements Dependency {

    public ServiceDependency(ServiceName name) {
        super(name);
    }

    public ServiceDependency(ServiceNameProvider provider) {
        super(provider.getServiceName());
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        builder.requires(this.getServiceName());
        return builder;
    }
}
