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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.remote.LocalEjbReceiver;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueInjectionService;

import java.util.List;

/**
 * @author Jaikiran Pai
 */
class EJBRemoteInvocationPassByValueWriteHandler extends AbstractWriteAttributeHandler<Void> {

    public static final EJBRemoteInvocationPassByValueWriteHandler INSTANCE = new EJBRemoteInvocationPassByValueWriteHandler(EJB3SubsystemRootResourceDefinition.PASS_BY_VALUE);

    private final AttributeDefinition attributeDefinition;

    private EJBRemoteInvocationPassByValueWriteHandler(final AttributeDefinition attributeDefinition) {
        super(attributeDefinition);
        this.attributeDefinition = attributeDefinition;
    }

    @Override
    protected void validateResolvedValue(String attributeName, ModelNode value) throws OperationFailedException {
        // we're going to validate using the AttributeDefinition in applyModelToRuntime, so don't bother here
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateDefaultLocalEJBReceiverService(context, model, null);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateDefaultLocalEJBReceiverService(context, restored, null);
    }

    void updateDefaultLocalEJBReceiverService(final OperationContext context, final ModelNode model, List<ServiceController<?>> newControllers) throws OperationFailedException {

        final ModelNode passByValueModel = this.attributeDefinition.resolveModelAttribute(context, model);
        final ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceName localEJBReceiverServiceName;
        if (passByValueModel.isDefined()) {
            final boolean passByValue = passByValueModel.asBoolean(true);
            if (passByValue) {
                localEJBReceiverServiceName = LocalEjbReceiver.BY_VALUE_SERVICE_NAME;
            } else {
                localEJBReceiverServiceName = LocalEjbReceiver.BY_REFERENCE_SERVICE_NAME;
            }
        } else {
            localEJBReceiverServiceName = LocalEjbReceiver.BY_VALUE_SERVICE_NAME;
        }
        // uninstall the existing default local EJB receiver service
        final ServiceController<?> existingDefaultLocalEJBReceiverServiceController = registry.getService(LocalEjbReceiver.DEFAULT_LOCAL_EJB_RECEIVER_SERVICE_NAME);
        if (existingDefaultLocalEJBReceiverServiceController != null) {
            context.removeService(existingDefaultLocalEJBReceiverServiceController);
        }
        final ServiceTarget serviceTarget = context.getServiceTarget();
        // now install the new default local EJB receiver service which points to a existing Local EJB receiver service
        final ValueInjectionService<LocalEjbReceiver> newDefaultLocalEJBReceiverService = new ValueInjectionService<LocalEjbReceiver>();
        final ServiceBuilder defaultLocalEJBReceiverServiceBuilder = serviceTarget.addService(LocalEjbReceiver.DEFAULT_LOCAL_EJB_RECEIVER_SERVICE_NAME, newDefaultLocalEJBReceiverService);
        defaultLocalEJBReceiverServiceBuilder.addDependency(localEJBReceiverServiceName, LocalEjbReceiver.class, newDefaultLocalEJBReceiverService.getInjector());
        // install the service
        final ServiceController defaultLocalEJBReceiverServiceController = defaultLocalEJBReceiverServiceBuilder.install();
        if (newControllers != null) {
            newControllers.add(defaultLocalEJBReceiverServiceController);
        }
    }

}
