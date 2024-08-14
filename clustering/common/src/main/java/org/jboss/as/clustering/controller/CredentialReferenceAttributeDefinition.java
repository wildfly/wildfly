/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller;

import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.dmr.ModelNode;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Provides a credential reference attribute, with the ability to resolve directly to a credential source dependency.
 * @author Paul Ferraro
 */
public class CredentialReferenceAttributeDefinition extends ObjectTypeAttributeDefinition implements ResourceModelResolver<ServiceDependency<CredentialSource>> {

    CredentialReferenceAttributeDefinition(Builder builder) {
        super(builder, builder.suffix, builder.valueTypes);
    }

    @Override
    public ServiceDependency<CredentialSource> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return ServiceDependency.from(CredentialReference.getCredentialSourceDependency(context, this, model));
    }

    public static class Builder extends AbstractAttributeDefinitionBuilder<Builder, CredentialReferenceAttributeDefinition> {
        private String suffix;
        private final AttributeDefinition[] valueTypes;

        public Builder(String name, RuntimeCapability<Void> capability) {
            this(CredentialReference.getAttributeBuilder(name, name, false, CapabilityReference.builder(capability, CommonServiceDescriptor.CREDENTIAL_STORE).build()).build());
        }

        private Builder(ObjectTypeAttributeDefinition attribute) {
            super(attribute);
            this.valueTypes = attribute.getValueTypes();
            this.setAttributeParser(AttributeParser.OBJECT_PARSER);
            this.setAttributeMarshaller(AttributeMarshaller.ATTRIBUTE_OBJECT);
        }

        public Builder setSuffix(String suffix) {
            this.suffix = suffix;
            return this;
        }

        @Override
        public CredentialReferenceAttributeDefinition build() {
            return new CredentialReferenceAttributeDefinition(this);
        }
    }
}
