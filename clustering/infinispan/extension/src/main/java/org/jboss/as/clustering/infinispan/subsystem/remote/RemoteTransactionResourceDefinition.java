/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

import java.util.EnumSet;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.CommonRequirement;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.UnaryCapabilityNameResolver;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
import org.jboss.as.clustering.controller.transform.RequiredChildResourceDiscardPolicy;
import org.jboss.as.clustering.controller.validation.EnumValidator;
import org.jboss.as.clustering.infinispan.subsystem.ComponentResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanModel;
import org.jboss.as.clustering.infinispan.subsystem.TransactionMode;
import org.jboss.as.clustering.infinispan.subsystem.TransactionResourceCapabilityReference;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;

/**
 * Resource definition for the transaction component of a remote cache container.
 * @author Paul Ferraro
 */
public class RemoteTransactionResourceDefinition extends ComponentResourceDefinition {

    public static final PathElement PATH = pathElement("transaction");

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        MODE("mode", ModelType.STRING, new ModelNode(TransactionMode.NONE.name())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new EnumValidator<>(TransactionMode.class, EnumSet.of(TransactionMode.NONE, TransactionMode.BATCH, TransactionMode.NON_DURABLE_XA)));
            }
        },
        TIMEOUT("timeout", ModelType.LONG, new ModelNode(60000L)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setMeasurementUnit(MeasurementUnit.MILLISECONDS);
            }
        },
        ;
        private final SimpleAttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        if (InfinispanModel.VERSION_9_0_0.requiresTransformation(version)) {
            parent.addChildResource(PATH, RequiredChildResourceDiscardPolicy.REJECT_AND_WARN);
        }
    }

    public RemoteTransactionResourceDefinition() {
        super(PATH);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(new RemoteTransactionResourceDefinition());
        Capability dependentCapability = new UnaryRequirementCapability(InfinispanRequirement.REMOTE_CONTAINER_CONFIGURATION, UnaryCapabilityNameResolver.PARENT);
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                // Add a requirement on the tm capability to the parent cache capability
                .addResourceCapabilityReference(new TransactionResourceCapabilityReference(dependentCapability, CommonRequirement.LOCAL_TRANSACTION_PROVIDER, Attribute.MODE, EnumSet.of(TransactionMode.NONE, TransactionMode.BATCH)))
                ;
        ResourceServiceHandler handler = new SimpleResourceServiceHandler(RemoteTransactionServiceConfigurator::new);
        new SimpleResourceRegistration(descriptor, handler).register(registration);
        return registration;
    }
}
