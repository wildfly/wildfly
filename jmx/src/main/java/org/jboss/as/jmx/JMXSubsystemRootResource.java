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

import java.util.Locale;

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
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JMXSubsystemRootResource extends SimpleResourceDefinition {

    static final JMXSubsystemRootResource INSTANCE = new JMXSubsystemRootResource();

    static final SimpleAttributeDefinition SHOW_MODEL = SimpleAttributeDefinitionBuilder.create(CommonAttributes.SHOW_MODEL, ModelType.BOOLEAN, true).setXmlName(CommonAttributes.VALUE).build();

    private static final String INVOKE_MBEAN_RAW = "invoke-mbean-raw";
    private static final String GET_MBEAN_INFO_RAW = "get-mbean-info-raw";
    private static final String GET_MBEAN_ATTRIBUTE_INFO_RAW = "get-mbean-attribute-info-raw";

    private JMXSubsystemRootResource() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, JMXExtension.SUBSYSTEM_NAME),
                JMXExtension.getResourceDescriptionResolver(JMXExtension.SUBSYSTEM_NAME),
                JMXSubsystemAdd.INSTANCE,
                JMXSubsystemRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(SHOW_MODEL, null, new JMXWriteAttributeHandler(SHOW_MODEL));
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(INVOKE_MBEAN_RAW, new InvokeMBeanRaw(), EMPTY_DESCRIPTION , false, EntryType.PRIVATE);
        resourceRegistration.registerOperationHandler(GET_MBEAN_INFO_RAW, new GetMBeanInfoRaw(), EMPTY_DESCRIPTION, false, EntryType.PRIVATE);
        resourceRegistration.registerOperationHandler(GET_MBEAN_ATTRIBUTE_INFO_RAW, new GetMBeanAttributeInfoRaw(), EMPTY_DESCRIPTION, false, EntryType.PRIVATE);
    }

    private static DescriptionProvider EMPTY_DESCRIPTION = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode();
        }
    };

    private static class JMXWriteAttributeHandler extends RestartParentWriteAttributeHandler {
        JMXWriteAttributeHandler(AttributeDefinition attr) {
            super(ModelDescriptionConstants.SUBSYSTEM, attr);
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel,
                ServiceVerificationHandler verificationHandler) throws OperationFailedException {
            JMXSubsystemAdd.INSTANCE.launchServices(context, parentModel, verificationHandler, null);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return MBeanServerService.SERVICE_NAME;
        }

    }
}
