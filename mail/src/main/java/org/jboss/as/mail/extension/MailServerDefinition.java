/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.DynamicNameMappers;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @since 7.1.0
 */
class MailServerDefinition extends PersistentResourceDefinition {
    static final String CREDENTIAL_STORE_CAPABILITY = "org.wildfly.security.credential-store";

    static final RuntimeCapability<Void> SERVER_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.mail.session.server", true)
            .setDynamicNameMapper(DynamicNameMappers.PARENT)
                    .build();

    static final SensitivityClassification MAIL_SERVER_SECURITY =
            new SensitivityClassification(MailExtension.SUBSYSTEM_NAME, "mail-server-security", false, false, true);

    static final SensitiveTargetAccessConstraintDefinition MAIL_SERVER_SECURITY_DEF = new SensitiveTargetAccessConstraintDefinition(MAIL_SERVER_SECURITY);

    static final SimpleAttributeDefinition OUTBOUND_SOCKET_BINDING_REF =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.OUTBOUND_SOCKET_BINDING_REF, ModelType.STRING, false)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
                    .build();

    static final SimpleAttributeDefinition OUTBOUND_SOCKET_BINDING_REF_OPTIONAL = SimpleAttributeDefinitionBuilder.create(OUTBOUND_SOCKET_BINDING_REF)
            .setCapabilityReference(OutboundSocketBinding.SERVICE_DESCRIPTOR.getName())
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setRequired(false)
            .build();

    protected static final SimpleAttributeDefinition SSL =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.SSL, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(ModelNode.FALSE)
                    .addAccessConstraint(MAIL_SERVER_SECURITY_DEF)
                    .build();


    protected static final SimpleAttributeDefinition TLS =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.TLS, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(ModelNode.FALSE)
                    .addAccessConstraint(MAIL_SERVER_SECURITY_DEF)
                    .build();

    protected static final SimpleAttributeDefinition USERNAME =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.USER_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setXmlName("username")
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
                    .addAccessConstraint(MAIL_SERVER_SECURITY_DEF)
                    .build();

    static final ObjectTypeAttributeDefinition CREDENTIAL_REFERENCE =
            CredentialReference.getAttributeBuilder(true, false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
                    .addAccessConstraint(MAIL_SERVER_SECURITY_DEF)
                    .addAlternatives(MailSubsystemModel.PASSWORD)
                    .setCapabilityReference(CREDENTIAL_STORE_CAPABILITY)
                    .build();

    protected static final SimpleAttributeDefinition PASSWORD =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.PASSWORD, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
                    .addAccessConstraint(MAIL_SERVER_SECURITY_DEF)
                    .setAlternatives(CredentialReference.CREDENTIAL_REFERENCE)
                    .build();

    static final PropertiesAttributeDefinition PROPERTIES = new PropertiesAttributeDefinition.Builder(ModelDescriptionConstants.PROPERTIES, true)
            .setAttributeMarshaller(AttributeMarshaller.PROPERTIES_MARSHALLER_UNWRAPPED)
            .setAttributeParser(AttributeParser.PROPERTIES_PARSER_UNWRAPPED)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();


    static final AttributeDefinition[] ATTRIBUTES = {OUTBOUND_SOCKET_BINDING_REF, SSL, TLS, USERNAME, PASSWORD, CREDENTIAL_REFERENCE};
    static final AttributeDefinition[] ATTRIBUTES_CUSTOM = {OUTBOUND_SOCKET_BINDING_REF_OPTIONAL, SSL, TLS, USERNAME, PASSWORD, CREDENTIAL_REFERENCE, PROPERTIES};


    private final List<AttributeDefinition> attributes;

    MailServerDefinition(final PathElement path, AttributeDefinition[] attributes) {
        super(new SimpleResourceDefinition.Parameters(path,
                MailExtension.getResourceDescriptionResolver(MailSubsystemModel.MAIL_SESSION, MailSubsystemModel.SERVER_TYPE))
                .setAddHandler(new MailServerAdd(attributes))
                .setAddRestartLevel(Flag.RESTART_RESOURCE_SERVICES)
                .setRemoveHandler(new MailServerRemove())
                .setRemoveRestartLevel(Flag.RESTART_RESOURCE_SERVICES)
                .setCapabilities(SERVER_CAPABILITY)
        );
        this.attributes = Arrays.asList(attributes);
    }


    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return attributes;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        MailServerWriteAttributeHandler handler = new MailServerWriteAttributeHandler(this.attributes);
        for (AttributeDefinition attr : getAttributes()) {
            resourceRegistration.registerReadWriteAttribute(attr, null, handler);
        }
    }
}
