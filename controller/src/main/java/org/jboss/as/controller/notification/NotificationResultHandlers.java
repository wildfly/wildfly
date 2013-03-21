/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.notification;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.OperationContext.ResultAction;
import static org.jboss.as.controller.OperationContext.ResultAction.KEEP;
import static org.jboss.as.controller.OperationContext.ResultHandler;
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_ADDED_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_REMOVED_NOTIFICATION;
import static org.jboss.as.controller.operations.global.GlobalNotifications.NEW_VALUE;
import static org.jboss.as.controller.operations.global.GlobalNotifications.OLD_VALUE;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * Result handlers to emit notifications when the operation is kept.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class NotificationResultHandlers {

    /**
     * Emit a {@code RESOURCE_ADDED_NOTIFICATION} if the result action is kept.
     */
    public static final ResultHandler RESOURCE_ADDED_RESULT_HANDLER = new ResultHandler() {
        @Override
        public void handleResult(ResultAction resultAction, OperationContext context, ModelNode operation) {
            if (resultAction != KEEP) {
                return;
            }
            PathAddress sourceAddress = pathAddress(operation.get(OP_ADDR));
            Notification notification = new Notification(RESOURCE_ADDED_NOTIFICATION,
                    sourceAddress,
                    MESSAGES.resourceWasAdded(sourceAddress));
            context.emit(notification);
        }
    };

    /**
     * Emit a {@code RESOURCE_REMOVED_NOTIFICATION} if the result action is kept.
     */
    public static final ResultHandler RESOURCE_REMOVED_RESULT_HANDLER = new ResultHandler() {
        @Override
        public void handleResult(ResultAction resultAction, OperationContext context, ModelNode operation) {
            if (resultAction != KEEP) {
                return;
            }
            PathAddress sourceAddress = pathAddress(operation.get(OP_ADDR));
            Notification notification = new Notification(RESOURCE_REMOVED_NOTIFICATION,
                    sourceAddress,
                    MESSAGES.resourceWasRemoved(sourceAddress));
            context.emit(notification);
        }
    };


    /**
     * Emit a {@code ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION} if the result action is kept.
     */
    public static final class AttributeValueWrittenResultHandler implements ResultHandler {

        private final String attributeName;
        private final ModelNode currentValue;
        private final ModelNode newValue;

        public AttributeValueWrittenResultHandler(String attributeName, ModelNode currentValue, ModelNode newValue) {
            this.attributeName = attributeName;
            this.currentValue = currentValue;
            this.newValue = newValue;
        }

        @Override
        public void handleResult(ResultAction resultAction, OperationContext context, ModelNode operation) {
            if (resultAction != KEEP) {
                return;
            }
            PathAddress sourceAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
            ModelNode data = new ModelNode();
            data.get(NAME).set(attributeName);
            data.get(OLD_VALUE).set(currentValue);
            data.get(NEW_VALUE).set(newValue);
            Notification notification = new Notification(ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION,
                    sourceAddress,
                    MESSAGES.attributeValueWritten(attributeName, currentValue, newValue),
                    data);
            context.emit(notification);
        }
    }

}
