/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.EnumSet;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.UnaryCapabilityNameResolver;
import org.jboss.as.clustering.controller.ReloadRequiredResourceRegistrar;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.controller.security.CredentialReferenceWriteAttributeHandler;
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
                .addAttributes(EnumSet.complementOf(EnumSet.of(Attribute.CREDENTIAL)))
                .addAttribute(Attribute.CREDENTIAL, new CredentialReferenceWriteAttributeHandler(Attribute.CREDENTIAL.getDefinition()))
                .addAttributes(SingleSignOnDefinition.Attribute.class)
                .addCapabilities(Capability.class)
                ;
        new ReloadRequiredResourceRegistrar(descriptor).register(registration);
    }
}
