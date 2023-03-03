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
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.common.function.ExceptionBiConsumer;

import jakarta.mail.Session;

/**
 * @author Tomaz Cerar
 * @created 19.12.11
 */
class MailSessionDefinition extends PersistentResourceDefinition {

    static final RuntimeCapability<Void> SESSION_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.mail.session", true, Session.class)
                .build();


    private final List<AccessConstraintDefinition> accessConstraints;

    protected static final SimpleAttributeDefinition JNDI_NAME =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.JNDI_NAME, ModelType.STRING, false)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
    protected static final SimpleAttributeDefinition FROM =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.FROM, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .build();
    protected static final SimpleAttributeDefinition DEBUG =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.DEBUG, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    static final AttributeDefinition[] ATTRIBUTES = {DEBUG, JNDI_NAME, FROM};

    MailSessionDefinition() {
        super(new SimpleResourceDefinition.Parameters(MailExtension.MAIL_SESSION_PATH,
                MailExtension.getResourceDescriptionResolver(MailSubsystemModel.MAIL_SESSION))
                .setAddHandler(new MailSessionAdd())
                .setRemoveHandler(new MailSessionRemove())
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES)
                .setCapabilities(SESSION_CAPABILITY)
        );
        ApplicationTypeConfig atc = new ApplicationTypeConfig(MailExtension.SUBSYSTEM_NAME, MailSubsystemModel.MAIL_SESSION);
        accessConstraints = new ApplicationTypeAccessConstraintDefinition(atc).wrapAsList();
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration registration) {
        ExceptionBiConsumer<OperationContext, ModelNode, OperationFailedException> remover = MailSessionRemove::removeSessionProviderService;
        ExceptionBiConsumer<OperationContext, ModelNode, OperationFailedException> installer = MailSessionAdd::installSessionProviderService;
        for (AttributeDefinition attribute : ATTRIBUTES) {
            if (!attribute.getName().equals(JNDI_NAME.getName())) {
                registration.registerReadWriteAttribute(attribute, null, new MailSessionWriteAttributeHandler(attribute, remover, installer));
            }
        }
        registration.registerReadWriteAttribute(JNDI_NAME, null, new MailSessionWriteAttributeHandler(JNDI_NAME, MailSessionRemove::removeBinderService, MailSessionAdd::installBinderService));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        return List.of(
                // /subsystem=mail/mail-session=java:/Mail/server=imap
                new MailServerDefinition(MailSubsystemModel.IMAP_SERVER_PATH, MailServerDefinition.ATTRIBUTES),
                // /subsystem=mail/mail-session=java:/Mail/server=pop3
                new MailServerDefinition(MailSubsystemModel.POP3_SERVER_PATH, MailServerDefinition.ATTRIBUTES),
                // /subsystem=mail/mail-session=java:/Mail/server=smtp
                new MailServerDefinition(MailSubsystemModel.SMTP_SERVER_PATH, MailServerDefinition.ATTRIBUTES),
                // /subsystem=mail/mail-session=java:/Mail/custom=*
                new MailServerDefinition(MailSubsystemModel.CUSTOM_SERVER_PATH, MailServerDefinition.ATTRIBUTES_CUSTOM)
        );
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }
}
