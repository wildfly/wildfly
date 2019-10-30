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
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.UnaryCapabilityNameResolver;
import org.jboss.as.clustering.infinispan.subsystem.ComponentResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelType;

/**
 * /subsystem=infinispan/remote-cache-container=X/component=security
 *
 * @author Radoslav Husar
 */
public class SecurityResourceDefinition extends ComponentResourceDefinition {

    public static final PathElement PATH = pathElement("security");

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        SSL_CONTEXT("ssl-context", ModelType.STRING, new CapabilityReference(Capability.SECURITY_SSL, CommonUnaryRequirement.SSL_CONTEXT)),
        ;

        private final AttributeDefinition definition;

        Attribute(String attributeName, ModelType type, CapabilityReference capabilityReference) {
            this.definition = new SimpleAttributeDefinitionBuilder(attributeName, type)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setAllowExpression(false)
                    .setCapabilityReference(capabilityReference)
                    .setValidator(new StringLengthValidator(1))
                    .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SSL_REF)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        SECURITY_SSL("org.wildfly.clustering.infinispan.remote-cache-container.security.ssl-context"),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name) {
            this.definition = RuntimeCapability.Builder.of(name, true).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();
        }

        @Override
        public RuntimeCapability<Void> getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        // No transformations yet
    }

    SecurityResourceDefinition() {
        super(PATH);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(Capability.class)
                ;
        ResourceServiceHandler handler = new SimpleResourceServiceHandler(SecurityServiceConfigurator::new);
        new SimpleResourceRegistration(descriptor, handler).register(registration);

        return registration;
    }
}
