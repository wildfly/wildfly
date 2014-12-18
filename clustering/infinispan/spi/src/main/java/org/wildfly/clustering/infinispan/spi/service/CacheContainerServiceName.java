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
 * Enumeration of service name factories for services associated with a cache container.
 * @author Paul Ferraro
 */
public enum CacheContainerServiceName implements CacheContainerServiceNameFactory {

    CACHE_CONTAINER {
        @Override
        public ServiceName getServiceName(String container) {
            return BASE_NAME.append(container);
        }
    },
    CONFIGURATION {
        @Override
        public ServiceName getServiceName(String container) {
            return CACHE_CONTAINER.getServiceName(container).append("config");
        }
    },
    AFFINITY {
        @Override
        public ServiceName getServiceName(String container) {
            return CACHE_CONTAINER.getServiceName(container).append("affinity");
        }
    },
    ;

    static final ServiceName BASE_NAME = ServiceName.JBOSS.append("infinispan");
}
