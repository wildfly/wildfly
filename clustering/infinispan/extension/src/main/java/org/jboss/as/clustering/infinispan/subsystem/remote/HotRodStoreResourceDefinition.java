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

package org.jboss.as.clustering.infinispan.subsystem.remote;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanExtension;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanModel;
import org.jboss.as.clustering.infinispan.subsystem.StoreResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;

/**
 * Resource description for the addressable resource:
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/store=hotrod
 *
 * @author Radoslav Husar
 */
public class HotRodStoreResourceDefinition extends StoreResourceDefinition {

    public static final PathElement PATH = pathElement("hotrod");

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        CACHE_CONFIGURATION("cache-configuration", ModelType.STRING, null),
        REMOTE_CACHE_CONTAINER("remote-cache-container", ModelType.STRING, new CapabilityReference(Capability.PERSISTENCE, InfinispanRequirement.REMOTE_CONTAINER)),
        ;

        private final AttributeDefinition definition;

        Attribute(String attributeName, ModelType type, CapabilityReference capabilityReference) {
            this.definition = new SimpleAttributeDefinitionBuilder(attributeName, type)
                    .setAllowExpression(capabilityReference == null)
                    .setRequired(capabilityReference != null)
                    .setCapabilityReference(capabilityReference)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    public static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        if (InfinispanModel.VERSION_7_0_0.requiresTransformation(version)) {
            parent.rejectChildResource(HotRodStoreResourceDefinition.PATH);
        }
    }

    public HotRodStoreResourceDefinition() {
        super(PATH, null, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, WILDCARD_PATH), new SimpleResourceDescriptorConfigurator<>(Attribute.class), HotRodStoreServiceConfigurator::new);
    }
}
