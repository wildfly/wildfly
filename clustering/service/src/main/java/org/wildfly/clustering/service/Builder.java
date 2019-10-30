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
package org.wildfly.clustering.service;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;

/**
 * Encapsulates the logic for building a service.
 * @author Paul Ferraro
 * @param <T> the type of value provided by services built by this builder
 * @deprecated Replaced by {@link ServiceConfigurator}.
 */
@Deprecated
public interface Builder<T> extends ServiceConfigurator {
    /**
     * Builds a service into the specified target.
     * @param target the service installation target
     * @return a service builder
     */
    @Override
    ServiceBuilder<T> build(ServiceTarget target);
}
