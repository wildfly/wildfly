/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.subsystem;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ejb3.cache.CacheFactoryBuilder;

/**
 * Defines a CacheFactoryBuilder instance which, during deployment, is used to configure, build and install a CacheFactory for the SFSB being deployed.
 * The CacheFactory resource instances defined here produce bean caches which are non distributed and do not have passivation-enabled.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class SimpleCacheFactoryResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    public static final String CACHE_FACTORY_CAPABILITY_NAME = "org.wildfly.ejb3.cache-factory";

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        SIMPLE_CACHE_FACTORY(CACHE_FACTORY_CAPABILITY_NAME)
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name) {
            this.definition = RuntimeCapability.Builder.of(CACHE_FACTORY_CAPABILITY_NAME, true, CacheFactoryBuilder.class)
                    .build();
        }

        @Override
        public RuntimeCapability<?> getDefinition() {
            return this.definition;
        }
    }

    public SimpleCacheFactoryResourceDefinition() {
        super(PathElement.pathElement(EJB3SubsystemModel.SIMPLE_CACHE), EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.SIMPLE_CACHE));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addCapabilities(Capability.class)
                ;
        SimpleCacheFactoryResourceServiceHandler handler = new SimpleCacheFactoryResourceServiceHandler();
        // register the child resource using its resource descriptor and resource service handler
        new SimpleResourceRegistration(descriptor, handler).register(registration);
        return registration;
    }
}
