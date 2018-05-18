/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory;

/**
 * @author Paul Ferraro
 */
public enum SingletonDefaultCacheRequirement implements UnaryRequirement {

    @Deprecated SINGLETON_SERVICE_BUILDER_FACTORY("org.wildfly.clustering.cache.default-singleton-service-builder-factory", SingletonServiceBuilderFactory.class),
    SINGLETON_SERVICE_CONFIGURATOR_FACTORY("org.wildfly.clustering.cache.default-singleton-service-configurator-factory", SingletonServiceConfiguratorFactory.class),
    ;
    private final String name;
    private final Class<?> type;

    SingletonDefaultCacheRequirement(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Class<?> getType() {
        return this.type;
    }
}
