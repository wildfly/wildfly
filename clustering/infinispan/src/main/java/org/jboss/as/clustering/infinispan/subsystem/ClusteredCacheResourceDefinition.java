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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Base class for cache resources which require common cache attributes and clustered cache attributes.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ClusteredCacheResourceDefinition extends CacheResourceDefinition {

    // attributes
    static final SimpleAttributeDefinition ASYNC_MARSHALLING = new SimpleAttributeDefinitionBuilder(ModelKeys.ASYNC_MARSHALLING, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.ASYNC_MARSHALLING.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(false))
            .build();

    static final SimpleAttributeDefinition MODE = new SimpleAttributeDefinitionBuilder(ModelKeys.MODE, ModelType.STRING, false)
            .setXmlName(Attribute.MODE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new EnumValidator<>(Mode.class, false, true))
            .build();

    static final SimpleAttributeDefinition QUEUE_FLUSH_INTERVAL = new SimpleAttributeDefinitionBuilder(ModelKeys.QUEUE_FLUSH_INTERVAL, ModelType.LONG, true)
            .setXmlName(Attribute.QUEUE_FLUSH_INTERVAL.getLocalName())
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(10L))
            .build();

    static final SimpleAttributeDefinition QUEUE_SIZE = new SimpleAttributeDefinitionBuilder(ModelKeys.QUEUE_SIZE, ModelType.INT, true)
            .setXmlName(Attribute.QUEUE_SIZE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(0))
            .build();

    static final SimpleAttributeDefinition REMOTE_TIMEOUT = new SimpleAttributeDefinitionBuilder(ModelKeys.REMOTE_TIMEOUT, ModelType.LONG, true)
            .setXmlName(Attribute.REMOTE_TIMEOUT.getLocalName())
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(17500L))
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { ASYNC_MARSHALLING, MODE, QUEUE_SIZE, QUEUE_FLUSH_INTERVAL, REMOTE_TIMEOUT };

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {

        if (InfinispanModel.VERSION_1_4_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, ASYNC_MARSHALLING, QUEUE_FLUSH_INTERVAL, QUEUE_SIZE, REMOTE_TIMEOUT);
        }

        CacheResourceDefinition.buildTransformation(version, builder);
    }

    ClusteredCacheResourceDefinition(CacheType type, ResolvePathHandler resolvePathHandler, boolean allowRuntimeOnlyRegistration) {
        super(type, resolvePathHandler, allowRuntimeOnlyRegistration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);
        OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute : ATTRIBUTES) {
            registration.registerReadWriteAttribute(attribute, null, writeHandler);
        }

        if (this.allowRuntimeOnlyRegistration) {
            OperationStepHandler handler = new ClusteredCacheMetricsHandler();
            for (ClusteredCacheMetric metric: ClusteredCacheMetric.values()) {
                registration.registerMetric(metric.getDefinition(), handler);
            }
        }
    }
}
