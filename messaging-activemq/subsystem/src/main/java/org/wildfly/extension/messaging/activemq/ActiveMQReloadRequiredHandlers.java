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