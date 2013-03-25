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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_ADDED_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_REMOVED_NOTIFICATION;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * Result handlers to emit notifications when the operation is kept.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class NotificationResultHandler implements ResultHandler {
    private final String type;
    private final String message;
    private final ModelNode data;

    public NotificationResultHandler(String type, String message) {
        this(type, message, null);
    }

    public NotificationResultHandler(String type, String message, ModelNode data) {
        this.type = type;
        this.message = message;
        this.data = data;
    }

    @Override
    public void handleResult(ResultAction resultAction, OperationContext context, ModelNode operation) {
        if (resultAction != KEEP) {
            return;
        }
        PathAddress address = pathAddress(operation.require(OP_ADDR));
        Notification notification = new Notification(type, address, message, data);
        context.emit(notification);
    }

    public static ResultHandler RESOURCE_ADDED_RESULT_HANDLER = new ResultHandler() {
        @Override
        public void handleResult(ResultAction resultAction, OperationContext context, ModelNode operation) {
            if (resultAction != KEEP) {
                return;
            }
            PathAddress address = pathAddress(operation.require(OP_ADDR));
            Notification notification = new Notification(RESOURCE_ADDED_NOTIFICATION, address, MESSAGES.resourceWasAdded(address));
            context.emit(notification);
        }
    };

    public static ResultHandler RESOURCE_REMOVED_RESULT_HANDLER = new ResultHandler() {
        @Override
        public void handleResult(ResultAction resultAction, OperationContext context, ModelNode operation) {
            if (resultAction != KEEP) {
                return;
            }
            PathAddress address = pathAddress(operation.require(OP_ADDR));
            Notification notification = new Notification(RESOURCE_REMOVED_NOTIFICATION, address, MESSAGES.resourceWasRemoved(address));
            context.emit(notification);
        }
    };
}
