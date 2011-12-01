/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jmx;

import static org.jboss.as.jmx.CommonAttributes.CONNECTOR;
import static org.jboss.as.jmx.CommonAttributes.JMX;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RestartParentWriteAttributeHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JMXConnectorResource extends SimpleResourceDefinition {

    static final PathElement CONNECTOR_CONFIG_PATH = PathElement.pathElement(CONNECTOR, JMX);

    static final JMXConnectorResource INSTANCE = new JMXConnectorResource();

    static final SimpleAttributeDefinition SERVER_BINDING =
            SimpleAttributeDefinitionBuilder.create(CommonAttributes.SERVER_BINDING, ModelType.STRING, false).setValidator(new StringLengthValidator(1, false)).build();
    static final SimpleAttributeDefinition REGISTRY_BINDING =
            SimpleAttributeDefinitionBuilder.create(CommonAttributes.REGISTRY_BINDING, ModelType.STRING, false).setValidator(new StringLengthValidator(1, false)).build();

    //private static final

    private JMXConnectorResource() {
        super(CONNECTOR_CONFIG_PATH,
                JMXExtension.getResourceDescriptionResolver(CommonAttributes.CONNECTOR),
                JMXConnectorAdd.INSTANCE,
                JMXConnectorRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(SERVER_BINDING, null, new JMXConnectorWriteAttributeHandler(SERVER_BINDING));
        resourceRegistration.registerReadWriteAttribute(REGISTRY_BINDING, null, new JMXConnectorWriteAttributeHandler(REGISTRY_BINDING));
    }

    private static class JMXConnectorWriteAttributeHandler extends RestartParentWriteAttributeHandler {
        JMXConnectorWriteAttributeHandler(AttributeDefinition attr) {
            super(ModelDescriptionConstants.SUBSYSTEM, attr);
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel,
                ServiceVerificationHandler verificationHandler) throws OperationFailedException {
            JMXConnectorAdd.INSTANCE.launchServices(context, parentModel, verificationHandler, null);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return JMXConnectorService.SERVICE_NAME;
        }

    }
}
