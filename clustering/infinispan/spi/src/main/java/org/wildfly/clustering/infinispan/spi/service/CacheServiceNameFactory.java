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
package org.wildfly.clustering.infinispan.spi.service;

import org.jboss.msc.service.ServiceName;

/**
 * Factory for generating service names for services associated with a cache.
 * @author Paul Ferraro
 */
public interface CacheServiceNameFactory extends CacheContainerServiceNameFactory {

    /**
     * The alias for the default cache of a cache container.
     */
    String DEFAULT_CACHE = "default";

    /**
     * Returns an appropriate service name for the specified container and cache names
     * @param container a container name
     * @param container a cache name
     * @return a service name
     */
    ServiceName getServiceName(String container, String cache);
}
