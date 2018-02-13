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

package org.jboss.as.clustering.infinispan.subsystem.remote;

import org.infinispan.client.hotrod.configuration.ExhaustedAction;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.validation.EnumValidator;
import org.jboss.as.clustering.infinispan.subsystem.ComponentResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * /subsystem=infinispan/remote-cache-container=X/component=connection-pool
 *
 * @author Radoslav Husar
 */
public class ConnectionPoolResourceDefinition extends ComponentResourceDefinition {

    public static final PathElement PATH = pathElement("connection-pool");

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        EXHAUSTED_ACTION("exhausted-action", ModelType.STRING, new ModelNode(ExhaustedAction.WAIT.name()), new EnumValidator<>(ExhaustedAction.class)),
        MAX_ACTIVE("max-active", ModelType.INT, new ModelNode(-1)),
        MAX_IDLE("max-idle", ModelType.INT, new ModelNode(-1)),
        MAX_TOTAL("max-total", ModelType.INT, new ModelNode(-1)),
        MAX_WAIT("max-wait", ModelType.LONG, new ModelNode(-1L)),
        MIN_EVICTABLE_IDLE_TIME("min-evictable-idle-time", ModelType.LONG, new ModelNode(1800000L)),
        MIN_IDLE("min-idle", ModelType.INT, new ModelNode(1)),
        NUM_TESTS_PER_EVICTION_RUN("num-tests-per-eviction-run", ModelType.INT, new ModelNode(3)),
        STRATEGY("strategy", ModelType.STRING, new ModelNode(ConnectionPoolStrategy.LIFO.name()), new EnumValidator<>(ConnectionPoolStrategy.class)),
        TEST_ON_BORROW("test-on-borrow", ModelType.BOOLEAN, new ModelNode(false)),
        TEST_ON_RETURN("test-on-return", ModelType.BOOLEAN, new ModelNode(false)),
        TEST_WHILE_IDLE("test-while-idle", ModelType.BOOLEAN, new ModelNode(true)),
        TIME_BETWEEN_EVICTION_RUNS("time-between-eviction-runs", ModelType.LONG, new ModelNode(120000L)),
        ;

        private final SimpleAttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = createBuilder(name, type, defaultValue).build();
        }

        Attribute(String name, ModelType type, ModelNode defaultValue, ModelTypeValidator validator) {
            SimpleAttributeDefinitionBuilder builder = createBuilder(name, type, defaultValue);
            this.definition = builder.setValidator(validator).build();
        }

        private static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type, ModelNode defaultValue) {
            return new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
                    ;
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        // No transformations yet
    }

    ConnectionPoolResourceDefinition() {
        super(PATH);
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        ManagementResourceRegistration subModel = registration.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class);
        ResourceServiceHandler handler = new SimpleResourceServiceHandler<>(ConnectionPoolBuilder::new);
        new SimpleResourceRegistration(descriptor, handler).register(subModel);
    }
}
