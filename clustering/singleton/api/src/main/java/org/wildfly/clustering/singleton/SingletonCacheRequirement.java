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

import org.wildfly.clustering.service.DefaultableBinaryRequirement;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * @author Paul Ferraro
 */
public enum SingletonCacheRequirement implements DefaultableBinaryRequirement {

    @Deprecated SINGLETON_SERVICE_BUILDER_FACTORY("org.wildfly.clustering.cache.singleton-service-builder-factory", SingletonDefaultCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY),
    SINGLETON_SERVICE_CONFIGURATOR_FACTORY("org.wildfly.clustering.cache.singleton-service-configurator-factory", SingletonDefaultCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY),
    ;
    private final String name;
    private final UnaryRequirement defaultRequirement;

    SingletonCacheRequirement(String name, UnaryRequirement defaultRequirement) {
        this.name = name;
        this.defaultRequirement = defaultRequirement;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public UnaryRequirement getDefaultRequirement() {
        return this.defaultRequirement;
    }
}
