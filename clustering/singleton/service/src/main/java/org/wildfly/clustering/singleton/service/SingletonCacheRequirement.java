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
package org.wildfly.clustering.singleton.service;

import org.jboss.as.clustering.controller.BinaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.BinaryServiceNameFactory;
import org.jboss.as.clustering.controller.DefaultableBinaryServiceNameFactoryProvider;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.wildfly.clustering.service.BinaryRequirement;

/**
 * @author Paul Ferraro
 */
public enum SingletonCacheRequirement implements DefaultableBinaryServiceNameFactoryProvider {
    @Deprecated(forRemoval = true) SINGLETON_SERVICE_BUILDER_FACTORY(org.wildfly.clustering.singleton.SingletonCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY, SingletonDefaultCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY),
    SINGLETON_SERVICE_CONFIGURATOR_FACTORY(org.wildfly.clustering.singleton.SingletonCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY, SingletonDefaultCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY),
    ;
    private final BinaryServiceNameFactory factory;
    private final SingletonDefaultCacheRequirement defaultRequirement;

    SingletonCacheRequirement(BinaryRequirement requirement, SingletonDefaultCacheRequirement defaultRequirement) {
        this.factory = new BinaryRequirementServiceNameFactory(requirement);
        this.defaultRequirement = defaultRequirement;
    }

    @Override
    public BinaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }

    @Override
    public UnaryServiceNameFactory getDefaultServiceNameFactory() {
        return this.defaultRequirement;
    }
}
