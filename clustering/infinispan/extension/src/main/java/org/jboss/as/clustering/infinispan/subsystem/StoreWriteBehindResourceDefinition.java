/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.AttributeConverter.DefaultValueAttributeConverter;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource
 *
 *    /subsystem=infinispan/cache-container=X/cache=Y/store=Z/write-behind=WRITE_BEHIND
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class StoreWriteBehindResourceDefinition extends StoreWriteResourceDefinition {

    static final PathElement LEGACY_PATH = PathElement.pathElement("write-behind", "WRITE_BEHIND");
    static final PathElement PATH = pathElement("behind");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        MODIFICATION_QUEUE_SIZE("modification-queue-size", ModelType.INT, new ModelNode(1024)),
        THREAD_POOL_SIZE("thread-pool-size", ModelType.INT, new ModelNode(1)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = createBuilder(name, type, defaultValue).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @Deprecated
    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        FLUSH_LOCK_TIMEOUT("flush-lock-timeout", ModelType.LONG, new ModelNode(5000L), InfinispanModel.VERSION_4_0_0),
        SHUTDOWN_TIMEOUT("shutdown-timeout", ModelType.LONG, new ModelNode(25000L), InfinispanModel.VERSION_4_0_0),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, ModelNode defaultValue, InfinispanModel deprecation) {
            this.definition = createBuilder(name, type, defaultValue).setDeprecated(deprecation.getVersion()).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type, ModelNode defaultValue) {
        return new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(defaultValue)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
        ;
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = InfinispanModel.VERSION_4_0_0.requiresTransformation(version) ? parent.addChildRedirection(PATH, LEGACY_PATH) : parent.addChildResource(PATH);

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().setValueConverter(new DefaultValueAttributeConverter(DeprecatedAttribute.FLUSH_LOCK_TIMEOUT.getDefinition()), DeprecatedAttribute.FLUSH_LOCK_TIMEOUT.getDefinition());
        }
    }

    StoreWriteBehindResourceDefinition() {
        super(PATH);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        parent.registerAlias(LEGACY_PATH, new SimpleAliasEntry(registration));

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addAttributes(DeprecatedAttribute.class)
                ;
        ResourceServiceHandler handler = new SimpleResourceServiceHandler(StoreWriteBehindServiceConfigurator::new);
        new SimpleResourceRegistration(descriptor, handler).register(registration);

        return registration;
    }
}
