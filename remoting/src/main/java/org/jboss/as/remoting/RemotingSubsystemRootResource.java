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
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RemotingSubsystemRootResource extends SimpleResourceDefinition {

    static final RemotingSubsystemRootResource INSTANCE = new RemotingSubsystemRootResource();

    //The defaults for these come from XnioWorker
    static final SimpleAttributeDefinition WORKER_READ_THREADS = createIntAttribute(CommonAttributes.WORKER_READ_THREADS, Attribute.WORKER_READ_THREADS, 1);
    static final SimpleAttributeDefinition WORKER_TASK_CORE_THREADS = createIntAttribute(CommonAttributes.WORKER_TASK_CORE_THREADS, Attribute.WORKER_TASK_CORE_THREADS, 4);
    static final SimpleAttributeDefinition WORKER_TASK_KEEPALIVE = createIntAttribute(CommonAttributes.WORKER_TASK_KEEPALIVE, Attribute.WORKER_TASK_KEEPALIVE, 60);
    static final SimpleAttributeDefinition WORKER_TASK_LIMIT = createIntAttribute(CommonAttributes.WORKER_TASK_LIMIT, Attribute.WORKER_TASK_LIMIT, 0x4000);
    static final SimpleAttributeDefinition WORKER_TASK_MAX_THREADS = createIntAttribute(CommonAttributes.WORKER_TASK_MAX_THREADS, Attribute.WORKER_TASK_MAX_THREADS, 16);
    static final SimpleAttributeDefinition WORKER_WRITE_THREADS = createIntAttribute(CommonAttributes.WORKER_WRITE_THREADS, Attribute.WORKER_WRITE_THREADS, 1);

    //private static final

    private RemotingSubsystemRootResource() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME),
                RemotingExtension.getResourceDescriptionResolver(RemotingExtension.SUBSYSTEM_NAME),
                RemotingSubsystemAdd.INSTANCE,
                RemotingSubsystemRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        registerReadWriteIntAttribute(resourceRegistration, WORKER_READ_THREADS);
        registerReadWriteIntAttribute(resourceRegistration, WORKER_TASK_CORE_THREADS);
        registerReadWriteIntAttribute(resourceRegistration, WORKER_TASK_KEEPALIVE);
        registerReadWriteIntAttribute(resourceRegistration, WORKER_TASK_LIMIT);
        registerReadWriteIntAttribute(resourceRegistration, WORKER_TASK_MAX_THREADS);
        registerReadWriteIntAttribute(resourceRegistration, WORKER_WRITE_THREADS);
    }

    private void registerReadWriteIntAttribute(ManagementResourceRegistration resourceRegistration, AttributeDefinition attr) {
        resourceRegistration.registerReadWriteAttribute(attr, null, new ThreadWriteAttributeHandler(attr));
    }

    private static SimpleAttributeDefinition createIntAttribute(String name, Attribute attribute, int defaultValue) {
        return SimpleAttributeDefinitionBuilder.create(name, ModelType.INT, true).setDefaultValue(new ModelNode().set(defaultValue)).setXmlName(attribute.getLocalName()).setValidator(new IntRangeValidator(1)).build();
    }

    private static class ThreadWriteAttributeHandler extends RestartParentWriteAttributeHandler {

        ThreadWriteAttributeHandler(AttributeDefinition definition) {
            super(CommonAttributes.SUBSYSTEM, definition);
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel,
                ServiceVerificationHandler verificationHandler) throws OperationFailedException {
            RemotingSubsystemAdd.INSTANCE.launchServices(context, parentModel, verificationHandler, null);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return RemotingServices.SUBSYSTEM_ENDPOINT;
        }

    }
}
