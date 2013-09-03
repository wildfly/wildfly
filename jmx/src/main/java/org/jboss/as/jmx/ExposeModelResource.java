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
import org.jboss.as.controller.access.management.JmxAuthorizer;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
abstract class ExposeModelResource extends SimpleResourceDefinition {

    private final ManagedAuditLogger auditLoggerInfo;
    private final JmxAuthorizer authorizer;
    private final SimpleAttributeDefinition domainName;

    ExposeModelResource(PathElement pathElement, ManagedAuditLogger auditLoggerInfo, JmxAuthorizer authorizer, SimpleAttributeDefinition domainName, SimpleAttributeDefinition...otherAttributes) {
        super(pathElement,
                JMXExtension.getResourceDescriptionResolver(CommonAttributes.EXPOSE_MODEL + "." + pathElement.getValue()),
                new ShowModelAdd(auditLoggerInfo, authorizer, domainName, otherAttributes),
                new ShowModelRemove(auditLoggerInfo, authorizer));
        this.auditLoggerInfo = auditLoggerInfo;
        this.authorizer = authorizer;
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

    class JMXWriteAttributeHandler extends RestartParentWriteAttributeHandler {
        JMXWriteAttributeHandler(AttributeDefinition attr) {
            super(ModelDescriptionConstants.SUBSYSTEM, attr);
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel,
                ServiceVerificationHandler verificationHandler) throws OperationFailedException {
            JMXSubsystemAdd.launchServices(context, parentModel, verificationHandler, auditLoggerInfo, authorizer, null);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return MBeanServerService.SERVICE_NAME;
        }
    }

    private static class ShowModelAdd extends RestartParentResourceAddHandler {

        private final ManagedAuditLogger auditLoggerInfo;
        private final JmxAuthorizer authorizer;
        private final SimpleAttributeDefinition domainName;
        private final SimpleAttributeDefinition[] otherAttributes;
        private ShowModelAdd(ManagedAuditLogger auditLoggerInfo, JmxAuthorizer authorizer, SimpleAttributeDefinition domainName, SimpleAttributeDefinition...otherAttributes) {
            super(ModelDescriptionConstants.SUBSYSTEM);
            this.auditLoggerInfo = auditLoggerInfo;
            this.authorizer = authorizer;
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
            JMXSubsystemAdd.launchServices(context, parentModel, verificationHandler, auditLoggerInfo, authorizer, null);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return MBeanServerService.SERVICE_NAME;
        }
    }

    private static class ShowModelRemove extends RestartParentResourceRemoveHandler {

        private final ManagedAuditLogger auditLoggerInfo;
        private final JmxAuthorizer authorizer;

        private ShowModelRemove(ManagedAuditLogger auditLoggerInfo, JmxAuthorizer authorizer) {
            super(ModelDescriptionConstants.SUBSYSTEM);
            this.auditLoggerInfo = auditLoggerInfo;
            this.authorizer = authorizer;
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel,
                ServiceVerificationHandler verificationHandler) throws OperationFailedException {
            JMXSubsystemAdd.launchServices(context, parentModel, verificationHandler, auditLoggerInfo, authorizer, null);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return MBeanServerService.SERVICE_NAME;
        }
    }
}
