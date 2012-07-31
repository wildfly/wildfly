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

import static org.jboss.as.jmx.JmxMessages.MESSAGES;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RestartParentResourceAddHandler;
import org.jboss.as.controller.RestartParentResourceRemoveHandler;
import org.jboss.as.controller.RestartParentWriteAttributeHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
abstract class ExposeModelResource extends SimpleResourceDefinition {

    private final SimpleAttributeDefinition domainName;

    ExposeModelResource(String modelName, SimpleAttributeDefinition domainName, SimpleAttributeDefinition...otherAttributes) {
        super(PathElement.pathElement(CommonAttributes.EXPOSE_MODEL, modelName),
                JMXExtension.getResourceDescriptionResolver(CommonAttributes.EXPOSE_MODEL + "." + modelName),
                new ShowModelAdd(domainName, otherAttributes),
                ShowModelRemove.INSTANCE);
        this.domainName = domainName;
    }

    static SimpleAttributeDefinition getDomainNameAttribute(String childName) {
        if (CommonAttributes.RESOLVED.equals(childName)){
            return ExposeModelResourceResolved.DOMAIN_NAME;
        } else if (CommonAttributes.EXPRESSION.equals(childName)) {
            return ExposeModelResourceExpression.DOMAIN_NAME;
        }

        throw MESSAGES.unknownChild(childName);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(domainName, null, new JMXWriteAttributeHandler(domainName));
    }

    static class JMXWriteAttributeHandler extends RestartParentWriteAttributeHandler {
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

    private static class ShowModelAdd extends RestartParentResourceAddHandler {

        private final SimpleAttributeDefinition domainName;
        private final SimpleAttributeDefinition[] otherAttributes;

        private ShowModelAdd(SimpleAttributeDefinition domainName, SimpleAttributeDefinition...otherAttributes) {
            super(ModelDescriptionConstants.SUBSYSTEM);
            this.domainName = domainName;
            this.otherAttributes = otherAttributes;
        }

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            domainName.validateAndSet(operation, model);
            if (otherAttributes.length > 0) {
                for (SimpleAttributeDefinition attr : otherAttributes) {
                    attr.validateAndSet(operation, model);
                }
            }
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

    private static class ShowModelRemove extends RestartParentResourceRemoveHandler {

        private static ShowModelRemove INSTANCE = new ShowModelRemove();

        private ShowModelRemove() {
            super(ModelDescriptionConstants.SUBSYSTEM);
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
