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
package org.wildfly.clustering.spi;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.SubGroupServiceNameFactory;

/**
 * Set of {@link ServiceName} factories for cache-based services.
 * @author Paul Ferraro
 */
public enum CacheGroupServiceName implements SubGroupServiceNameFactory {
    NODE_FACTORY() {
        @Override
        public ServiceName getServiceName(String container, String cache) {
            return GroupServiceName.NODE_FACTORY.getServiceName(container).append((cache != null) ? cache : DEFAULT_SUB_GROUP);
        }
    },
    GROUP() {
        @Override
        public ServiceName getServiceName(String container, String cache) {
            return GroupServiceName.GROUP.getServiceName(container).append((cache != null) ? cache : DEFAULT_SUB_GROUP);
        }
    },
    REGISTRY() {
        @Override
        public ServiceName getServiceName(String container, String cache) {
            return GroupServiceName.BASE_SERVICE_NAME.append(this.toString(), container, (cache != null) ? cache : DEFAULT_SUB_GROUP);
        }

        @Override
        public String toString() {
            return "registry";
        }
    },
    REGISTRY_ENTRY() {
        @Override
        public ServiceName getServiceName(String container, String cache) {
            return REGISTRY.getServiceName(container, cache).append("entry");
        }
    },
    REGISTRY_FACTORY() {
        @Override
        public ServiceName getServiceName(String container, String cache) {
            return REGISTRY.getServiceName(container, cache).append("factory");
        }
    },
    SERVICE_PROVIDER_REGISTRY() {
        @Override
        public ServiceName getServiceName(String container, String cache) {
            return GroupServiceName.BASE_SERVICE_NAME.append(this.toString(), container, (cache != null) ? cache : DEFAULT_SUB_GROUP);
        }

        @Override
        public String toString() {
            return "providers";
        }
    },
    ;
    private static final String DEFAULT_SUB_GROUP = GroupServiceName.DEFAULT_GROUP;
}
