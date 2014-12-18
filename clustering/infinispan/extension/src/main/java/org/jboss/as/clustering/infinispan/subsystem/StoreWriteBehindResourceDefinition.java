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

import org.jboss.as.clustering.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.clustering.controller.transform.DefaultValueAttributeConverter;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
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
public class StoreWriteBehindResourceDefinition extends SimpleResourceDefinition {

    static final PathElement PATH = PathElement.pathElement(ModelKeys.WRITE_BEHIND, ModelKeys.WRITE_BEHIND_NAME);

    // attributes
    static final SimpleAttributeDefinition FLUSH_LOCK_TIMEOUT = new SimpleAttributeDefinitionBuilder(ModelKeys.FLUSH_LOCK_TIMEOUT, ModelType.LONG, true)
            .setXmlName(Attribute.FLUSH_LOCK_TIMEOUT.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(5000L))
            .build();

    static final SimpleAttributeDefinition MODIFICATION_QUEUE_SIZE = new SimpleAttributeDefinitionBuilder(ModelKeys.MODIFICATION_QUEUE_SIZE, ModelType.INT, true)
            .setXmlName(Attribute.MODIFICATION_QUEUE_SIZE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(1024))
            .build();

    static final SimpleAttributeDefinition SHUTDOWN_TIMEOUT = new SimpleAttributeDefinitionBuilder(ModelKeys.SHUTDOWN_TIMEOUT, ModelType.LONG, true)
            .setXmlName(Attribute.SHUTDOWN_TIMEOUT.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(25000L))
            .build();

    static final SimpleAttributeDefinition THREAD_POOL_SIZE = new SimpleAttributeDefinitionBuilder(ModelKeys.THREAD_POOL_SIZE, ModelType.INT, true)
            .setXmlName(Attribute.THREAD_POOL_SIZE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(1))
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] {
            FLUSH_LOCK_TIMEOUT, MODIFICATION_QUEUE_SIZE, THREAD_POOL_SIZE, SHUTDOWN_TIMEOUT
    };

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(PATH);

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().setValueConverter(new DefaultValueAttributeConverter(FLUSH_LOCK_TIMEOUT), FLUSH_LOCK_TIMEOUT);
        }

        if (InfinispanModel.VERSION_1_4_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, FLUSH_LOCK_TIMEOUT, MODIFICATION_QUEUE_SIZE, SHUTDOWN_TIMEOUT, THREAD_POOL_SIZE);
        }
    }

    StoreWriteBehindResourceDefinition() {
        super(PATH, InfinispanExtension.getResourceDescriptionResolver(ModelKeys.WRITE_BEHIND),
                new ReloadRequiredAddStepHandler(ATTRIBUTES), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            registration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }
}
