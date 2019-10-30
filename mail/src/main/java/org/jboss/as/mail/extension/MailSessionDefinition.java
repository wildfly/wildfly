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
import javax.mail.Session;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

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
                    .setRestartAllServices()
                    .build();
    protected static final SimpleAttributeDefinition FROM =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.FROM, ModelType.STRING, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .setRequired(false)
                    .build();
    protected static final SimpleAttributeDefinition DEBUG =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.DEBUG, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .setRestartAllServices()
                    .build();

    static final AttributeDefinition[] ATTRIBUTES = {DEBUG, JNDI_NAME, FROM};

    private static final List<MailServerDefinition> CHILDREN = Arrays.asList(
            // /subsystem=mail/mail-session=java:/Mail/server=imap
            MailServerDefinition.INSTANCE_IMAP,
            // /subsystem=mail/mail-session=java:/Mail/server=pop3
            MailServerDefinition.INSTANCE_POP3,
            // /subsystem=mail/mail-session=java:/Mail/server=smtp
            MailServerDefinition.INSTANCE_SMTP,
            // /subsystem=mail/mail-session=java:/Mail/custom=*
            MailServerDefinition.INSTANCE_CUSTOM

    );

    static final MailSessionDefinition INSTANCE = new MailSessionDefinition();

    private MailSessionDefinition() {
        super(new Parameters(MailExtension.MAIL_SESSION_PATH,
                MailExtension.getResourceDescriptionResolver(MailSubsystemModel.MAIL_SESSION))
                .setAddHandler(MailSessionAdd.INSTANCE)
                .setRemoveHandler(new ServiceRemoveStepHandler(MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName(), MailSessionAdd.INSTANCE))
                .setCapabilities(SESSION_CAPABILITY)
        );
        ApplicationTypeConfig atc = new ApplicationTypeConfig(MailExtension.SUBSYSTEM_NAME, MailSubsystemModel.MAIL_SESSION);
        accessConstraints = new ApplicationTypeAccessConstraintDefinition(atc).wrapAsList();
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration rootResourceRegistration) {
        MailServerWriteAttributeHandler handler = new MailServerWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            rootResourceRegistration.registerReadWriteAttribute(attr, null, handler);
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        return CHILDREN;
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

}
