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
package org.jboss.as.remoting;

import static org.jboss.as.remoting.CommonAttributes.CONNECTOR;
import static org.jboss.as.remoting.CommonAttributes.PROPERTY;

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
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class PropertyResource extends SimpleResourceDefinition {

    static final PropertyResource INSTANCE_CONNECTOR = new PropertyResource(CONNECTOR);

    static final SimpleAttributeDefinition VALUE = new NamedValueAttributeDefinition(CommonAttributes.VALUE, Attribute.VALUE, null, ModelType.STRING, true);
    private final String parent;
    protected PropertyResource(String parent) {
        super(PathElement.pathElement(PROPERTY),
                RemotingExtension.getResourceDescriptionResolver(PROPERTY),
                new PropertyAdd(parent),
                new PropertyRemove(parent));
        this.parent = parent;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(VALUE, null, new PropertyWriteAttributeHandler(parent));
    }

    private static void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel,
            ServiceVerificationHandler verificationHandler) {
        try {
            ConnectorAdd.INSTANCE.launchServices(context, parentAddress, parentAddress.getLastElement().getValue(), parentModel, verificationHandler, null);
        } catch (OperationFailedException e) {
            //TODO handle better?
            throw new RuntimeException(e);
        }
    }

    private static ServiceName getParentServiceName(PathAddress parentAddress) {
        return RemotingServices.serverServiceName(parentAddress.getLastElement().getValue());
    }

    private static class PropertyWriteAttributeHandler extends RestartParentWriteAttributeHandler {

        public PropertyWriteAttributeHandler(String parent) {
            // FIXME PropertyWriteAttributeHandler constructor
            super(parent, VALUE);
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel,
                ServiceVerificationHandler verificationHandler) throws OperationFailedException {
            PropertyResource.recreateParentService(context, parentAddress, parentModel, verificationHandler);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return PropertyResource.getParentServiceName(parentAddress);
        }
    }

    private static class PropertyAdd extends RestartParentResourceAddHandler {

        private PropertyAdd(String parent) {
            super(parent);
        }

        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException{
            PropertyResource.VALUE.validateAndSet(operation, model);
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel,
                ServiceVerificationHandler verificationHandler) {
            PropertyResource.recreateParentService(context, parentAddress, parentModel, verificationHandler);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return PropertyResource.getParentServiceName(parentAddress);
        }
    }

    private static class PropertyRemove extends RestartParentResourceRemoveHandler {
        private PropertyRemove(String parent) {
            super(parent);
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel,
                ServiceVerificationHandler verificationHandler)  {
            PropertyResource.recreateParentService(context, parentAddress, parentModel, verificationHandler);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return PropertyResource.getParentServiceName(parentAddress);
        }
    }

}
