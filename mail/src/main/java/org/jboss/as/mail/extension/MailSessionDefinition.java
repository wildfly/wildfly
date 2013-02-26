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

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author Tomaz Cerar
 * @created 19.12.11 21:04
 */
class MailSessionDefinition extends SimpleResourceDefinition {
    public static MailSessionDefinition INSTANCE = new MailSessionDefinition();

    private MailSessionDefinition() {
        super(MailExtension.MAIL_SESSION_PATH,
                MailExtension.getResourceDescriptionResolver(MailSubsystemModel.MAIL_SESSION),
                MailSessionAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    protected static final SimpleAttributeDefinition JNDI_NAME =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.JNDI_NAME, ModelType.STRING, false)
                    .setAllowExpression(true)
                    .setRestartAllServices()
                    .build();
    protected static final SimpleAttributeDefinition FROM =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.FROM, ModelType.STRING, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .build();
    protected static final SimpleAttributeDefinition DEBUG =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.DEBUG, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .setRestartAllServices()
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = {DEBUG, JNDI_NAME, FROM};

    @Override
    public void registerAttributes(final ManagementResourceRegistration rootResourceRegistration) {
        ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            rootResourceRegistration.registerReadWriteAttribute(attr, null, handler);
        }
    }

    private static class SessionAttributeWriteHandler extends AbstractWriteAttributeHandler {
        protected static SessionAttributeWriteHandler INSTANCE = new SessionAttributeWriteHandler();

        private SessionAttributeWriteHandler() {
            super(ATTRIBUTES);
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder handbackHolder) throws OperationFailedException {
            String jndiName = JNDI_NAME.resolveModelAttribute(context, context.readResource(PathAddress.EMPTY_ADDRESS).getModel()).asString();
            final ServiceName serviceName = MailSessionAdd.SERVICE_NAME_BASE.append(jndiName);
            AttributeDefinition def = getAttributeDefinition(attributeName);
            ServiceController svcCtrl = context.getServiceRegistry(false).getService(serviceName);
            context.removeService(svcCtrl);
            MailSessionService service = (MailSessionService) svcCtrl.getService();
            if (def == DEBUG) {
                service.getConfig().setDebug(resolvedValue.asBoolean());
            } else if (def == FROM) {
                service.getConfig().setFrom(resolvedValue.asString());
            }
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Object handback) throws OperationFailedException {

        }
    }


}
