/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.UnaryCapabilityNameResolver;
import org.jboss.as.clustering.controller.ReloadRequiredResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
public class ApplicationSecurityDomainSingleSignOnDefinition extends SingleSignOnDefinition {

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        SSO_CREDENTIAL_STORE("org.wildfly.extension.undertow.application-security-domain.single-sign-on.credential-store"),
        SSO_KEY_STORE("org.wildfly.extension.undertow.application-security-domain.single-sign-on.key-store"),
        SSO_SSL_CONTEXT("org.wildfly.extension.undertow.application-security-domain.single-sign-on.client-ssl-context"),
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

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        CREDENTIAL(CredentialReference.getAttributeBuilder(CredentialReference.CREDENTIAL_REFERENCE, CredentialReference.CREDENTIAL_REFERENCE, false, new CapabilityReference(Capability.SSO_CREDENTIAL_STORE, CommonUnaryRequirement.CREDENTIAL_STORE)).setAccessConstraints(SensitiveTargetAccessConstraintDefinition.CREDENTIAL).build()),
        KEY_ALIAS("key-alias", ModelType.STRING, builder -> builder.setAllowExpression(true).addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SSL_REF)),
        KEY_STORE("key-store", ModelType.STRING, builder -> builder.setCapabilityReference(new CapabilityReference(Capability.SSO_KEY_STORE, CommonUnaryRequirement.KEY_STORE)).addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SSL_REF)),
        SSL_CONTEXT("client-ssl-context", ModelType.STRING, builder -> builder.setRequired(false).setCapabilityReference(new CapabilityReference(Capability.SSO_SSL_CONTEXT, CommonUnaryRequirement.SSL_CONTEXT)).setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SSL_REF)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, UnaryOperator<SimpleAttributeDefinitionBuilder> configurator) {
            this.definition = configurator.apply(new SimpleAttributeDefinitionBuilder(name, type).setRequired(true)).build();
        }

        Attribute(AttributeDefinition definition) {
            this.definition = definition;
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addAttributes(SingleSignOnDefinition.Attribute.class)
                .addCapabilities(Capability.class)
                ;
        new ReloadRequiredResourceRegistration(descriptor).register(registration);
    }
}
