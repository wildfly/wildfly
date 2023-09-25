/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.concurrent.TimeUnit;

import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/expiration=EXPIRATION
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ExpirationResourceDefinition extends ComponentResourceDefinition {

    static final PathElement PATH = pathElement("expiration");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        INTERVAL("interval", new ModelNode(TimeUnit.MINUTES.toMillis(1))),
        LIFESPAN("lifespan", null),
        MAX_IDLE("max-idle", null),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, ModelType.LONG)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    ExpirationResourceDefinition() {
        super(PATH);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver()).addAttributes(Attribute.class);
        ResourceServiceHandler handler = new SimpleResourceServiceHandler(ExpirationServiceConfigurator::new);
        new SimpleResourceRegistrar(descriptor, handler).register(registration);

        return registration;
    }
}
