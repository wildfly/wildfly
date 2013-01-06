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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Base class for cache resources which require common cache attributes and clustered cache attributes.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ClusteredCacheResource extends CacheResource  {

    // attributes
    static final SimpleAttributeDefinition ASYNC_MARSHALLING =
            new SimpleAttributeDefinitionBuilder(ModelKeys.ASYNC_MARSHALLING, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.ASYNC_MARSHALLING.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(false))
                    .build();

    // the attribute definition for the cache mode
    static final SimpleAttributeDefinition MODE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.MODE, ModelType.STRING, false)
                    .setXmlName(Attribute.MODE.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new EnumValidator<Mode>(Mode.class, false, true))
                    .build();

    static final SimpleAttributeDefinition QUEUE_FLUSH_INTERVAL =
            new SimpleAttributeDefinitionBuilder(ModelKeys.QUEUE_FLUSH_INTERVAL, ModelType.LONG, true)
                    .setXmlName(Attribute.QUEUE_FLUSH_INTERVAL.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(10))
                    .build();

    static final SimpleAttributeDefinition QUEUE_SIZE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.QUEUE_SIZE, ModelType.INT, true)
                    .setXmlName(Attribute.QUEUE_SIZE.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(0))
                    .build();

    static final SimpleAttributeDefinition REMOTE_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(ModelKeys.REMOTE_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(Attribute.REMOTE_TIMEOUT.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(17500))
                    .build();

    static final AttributeDefinition[] CLUSTERED_CACHE_ATTRIBUTES = { ASYNC_MARSHALLING, MODE, QUEUE_SIZE, QUEUE_FLUSH_INTERVAL, REMOTE_TIMEOUT};


    public ClusteredCacheResource(PathElement pathElement, ResourceDescriptionResolver descriptionResolver, AbstractAddStepHandler addHandler, OperationStepHandler removeHandler, ResolvePathHandler resolvePathHandler) {
        super(pathElement, descriptionResolver, addHandler, removeHandler, resolvePathHandler);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // do we really need a special handler here?
        final OperationStepHandler writeHandler = new CacheWriteAttributeHandler(CLUSTERED_CACHE_ATTRIBUTES);
        for (AttributeDefinition attr : CLUSTERED_CACHE_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, CacheReadAttributeHandler.INSTANCE, writeHandler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }
}
