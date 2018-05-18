/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.singleton;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.Builder;

/**
 * Defines a singleton policy.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link org.wildfly.clustering.singleton.service.SingletonPolicy}.
 */
@Deprecated
public interface SingletonPolicy extends org.wildfly.clustering.singleton.service.SingletonPolicy {

    /**
     * Creates a singleton service builder.
     * @param name the name of the service
     * @param service the service to run when elected as the primary node
     * @return a builder
     * @deprecated Use {@link #createSingletonServiceConfigurator(ServiceName)} instead.
     */
    @Deprecated
    <T> Builder<T> createSingletonServiceBuilder(ServiceName name, Service<T> service);

    /**
     * Creates a singleton service builder.
     * @param name the name of the service
     * @param primaryService the service to run when elected as the primary node
     * @param backupService the service to run when not elected as the primary node
     * @return a builder
     * @deprecated Use {@link #createSingletonServiceConfigurator(ServiceName)} instead.
     */
    @Deprecated
    <T> Builder<T> createSingletonServiceBuilder(ServiceName name, Service<T> primaryService, Service<T> backupService);
}
