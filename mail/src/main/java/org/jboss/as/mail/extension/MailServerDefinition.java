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

package org.jboss.as.mail.extension;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.RestartParentResourceRemoveHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

/**
 * @author Tomaz Cerar
 * @since 7.1.0
 */
class MailServerDefinition extends PersistentResourceDefinition {

    static final SensitivityClassification MAIL_SERVER_SECURITY =
            new SensitivityClassification(MailExtension.SUBSYSTEM_NAME, "mail-server-security", false, false, true);

    static final SensitiveTargetAccessConstraintDefinition MAIL_SERVER_SECURITY_DEF = new SensitiveTargetAccessConstraintDefinition(MAIL_SERVER_SECURITY);

    protected static final SimpleAttributeDefinition OUTBOUND_SOCKET_BINDING_REF =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.OUTBOUND_SOCKET_BINDING_REF, ModelType.STRING, false)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
                    .build();

    protected static final SimpleAttributeDefinition OUTBOUND_SOCKET_BINDING_REF_OPTIONAL = SimpleAttributeDefinitionBuilder.create(OUTBOUND_SOCKET_BINDING_REF)
            .setAllowNull(true)
            .build();

    protected static final SimpleAttributeDefinition SSL =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.SSL, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .addAccessConstraint(MAIL_SERVER_SECURITY_DEF)
                    .build();


    protected static final SimpleAttributeDefinition TLS =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.TLS, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .addAccessConstraint(MAIL_SERVER_SECURITY_DEF)
                    .build();

    protected static final SimpleAttributeDefinition USERNAME =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.USER_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setXmlName("username")
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
                    .addAccessConstraint(MAIL_SERVER_SECURITY_DEF)
                    .build();

    protected static final SimpleAttributeDefinition PASSWORD =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.PASSWORD, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
                    .addAccessConstraint(MAIL_SERVER_SECURITY_DEF)
                    .build();

    protected static final PropertiesAttributeDefinition PROPERTIES = new PropertiesAttributeDefinition.Builder(ModelDescriptionConstants.PROPERTIES, true)
            .setXmlName("property")
            .setWrapXmlElement(false)
            .setAllowExpression(true)
            .build();


    static final AttributeDefinition[] ATTRIBUTES = {OUTBOUND_SOCKET_BINDING_REF, SSL, TLS, USERNAME, PASSWORD};
    static final AttributeDefinition[] ATTRIBUTES_CUSTOM = {OUTBOUND_SOCKET_BINDING_REF_OPTIONAL, SSL, TLS, USERNAME, PASSWORD, PROPERTIES};


    public static final MailServerDefinition INSTANCE_SMTP = new MailServerDefinition(MailSubsystemModel.SMTP_SERVER_PATH, ATTRIBUTES);
    public static final MailServerDefinition INSTANCE_IMAP = new MailServerDefinition(MailSubsystemModel.IMAP_SERVER_PATH, ATTRIBUTES);
    public static final MailServerDefinition INSTANCE_POP3 = new MailServerDefinition(MailSubsystemModel.POP3_SERVER_PATH, ATTRIBUTES);
    public static final MailServerDefinition INSTANCE_CUSTOM = new MailServerDefinition(MailSubsystemModel.CUSTOM_SERVER_PATH, ATTRIBUTES_CUSTOM);

    private final List<AttributeDefinition> attributes;

    private MailServerDefinition(final PathElement path, AttributeDefinition[] attributes) {
        super(path,
                MailExtension.getResourceDescriptionResolver(MailSubsystemModel.MAIL_SESSION, MailSubsystemModel.SERVER_TYPE),
                new MailServerAdd(attributes),
                new MailServerRemove());
        this.attributes = Arrays.asList(attributes);
    }


    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return attributes;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        MailServerWriteAttributeHandler handler = new MailServerWriteAttributeHandler(getAttributes());
        for (AttributeDefinition attr : getAttributes()) {
            resourceRegistration.registerReadWriteAttribute(attr, null, handler);
        }
    }

    private static final class MailServerRemove extends RestartParentResourceRemoveHandler {
        private MailServerRemove() {
            super(MailSubsystemModel.MAIL_SESSION);
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
            MailSessionAdd.installRuntimeServices(context, parentAddress, parentModel);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return MailSessionAdd.MAIL_SESSION_SERVICE_NAME.append(parentAddress.getLastElement().getValue());
        }

        @Override
        protected void removeServices(OperationContext context, ServiceName parentService, ModelNode parentModel) throws OperationFailedException {
            super.removeServices(context, parentService, parentModel);
            String jndiName = MailSessionAdd.getJndiName(parentModel, context);
            final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
            context.removeService(bindInfo.getBinderServiceName());
        }


    }
}
