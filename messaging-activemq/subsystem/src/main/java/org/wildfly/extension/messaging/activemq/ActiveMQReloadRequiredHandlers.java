/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import java.util.Collection;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Requires a reload only if the {@link ActiveMQServerService} service is up and running.
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat, inc
 */
public interface ActiveMQReloadRequiredHandlers {
    static class AddStepHandler extends AbstractAddStepHandler {

        private boolean reloadRequired = false;

        public AddStepHandler(Collection<? extends AttributeDefinition> attributes) {
            super(attributes);
        }

        public AddStepHandler(AttributeDefinition... attributes) {
            super(attributes);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            if (isServiceInstalled(context)) {
                context.reloadRequired();
                reloadRequired = true;
            }
        }

        @Override
        protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
            if (reloadRequired && isServiceInstalled(context)) {
                context.revertReloadRequired();
            }
        }
    }

    final class RemoveStepHandler extends AbstractRemoveStepHandler {

        private boolean reloadRequired = false;

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            if (isServiceInstalled(context)) {
                context.reloadRequired();
                reloadRequired = true;
            }
        }

        @Override
        protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            if (reloadRequired && isServiceInstalled(context)) {
                context.revertReloadRequired();
            }
        }
    }

    static class WriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

        public WriteAttributeHandler(Collection<? extends AttributeDefinition> definitions) {
            super(definitions.toArray(new AttributeDefinition[definitions.size()]));
        }

        public WriteAttributeHandler(AttributeDefinition... definitions) {
            super(definitions);
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                ModelNode resolvedValue, ModelNode currentValue,
                HandbackHolder<Void> handbackHolder)
                throws OperationFailedException {
            return isServiceInstalled(context);
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
            if (isServiceInstalled(context)) {
                context.revertReloadRequired();
            }
        }
    }

    /**
     * Returns true if a {@link ServiceController} for this service has been {@link org.jboss.msc.service.ServiceBuilder#install() installed}
     * in MSC under the
     * {@link MessagingServices#getActiveMQServiceName(org.jboss.as.controller.PathAddress) service name appropriate to the given operation}.
     *
     * @param context the operation context
     * @return {@code true} if a {@link ServiceController} is installed
     */
    static boolean isServiceInstalled(final OperationContext context) {
        if (context.isNormalServer()) {
            final ServiceName serviceName = MessagingServices.getActiveMQServiceName(context.getCurrentAddress());
            if (serviceName != null) {
                return context.getServiceRegistry(false).getService(serviceName) != null;
            }
        }
        return false;
    }
}