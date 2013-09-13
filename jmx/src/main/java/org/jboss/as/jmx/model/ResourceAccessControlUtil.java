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
package org.jboss.as.jmx.model;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_CONTROL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXCEPTIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXECUTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INHERITED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE;
import static org.jboss.as.jmx.JmxMessages.MESSAGES;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.ReadResourceDescriptionHandler;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class ResourceAccessControlUtil {

    private static final ResourceAccessControl NOT_ADDRESSABLE;
    static {
        ModelNode notAddressable = new ModelNode();
        notAddressable.get(ADDRESS).set(false);
        notAddressable.protect();
        NOT_ADDRESSABLE = new ResourceAccessControl(notAddressable);
    }

    private final ModelController controller;

    ResourceAccessControlUtil(ModelController controller) {
        this.controller = controller;
    }

    ResourceAccessControl getResourceAccessWithInstanceNotFoundExceptionIfNotAccessible(ObjectName name, PathAddress address, boolean operations) throws InstanceNotFoundException {
        ResourceAccessControl accessControl = getResourceAccess(address, operations);
        if (!accessControl.isAccessibleResource()) {
            throw MESSAGES.mbeanNotFound(name);
        }
        return accessControl;
    }

    ResourceAccessControl getResourceAccess(PathAddress address, boolean operations) {
        ModelNode op = Util.createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, address);
        op.get(ACCESS_CONTROL).set(ReadResourceDescriptionHandler.AccessControl.TRIM_DESCRIPTONS.toModelNode());
        if (operations) {
            op.get(OPERATIONS).set(true);
            op.get(INHERITED).set(false);
        }

        ModelNode result = controller.execute(op, null, ModelController.OperationTransactionControl.COMMIT, null);
        if (!result.get(OUTCOME).asString().equals(SUCCESS)) {
            return NOT_ADDRESSABLE;
        } else {
            final ModelNode accessControl = result.get(RESULT, ACCESS_CONTROL);
            ModelNode useAccessControl = null;
            if (accessControl.hasDefined(EXCEPTIONS) && accessControl.get(EXCEPTIONS).keys().size() > 0) {
                String key = address.toModelNode().asString();
                ModelNode exception = accessControl.get(EXCEPTIONS, key);
                if (exception.isDefined()) {
                    useAccessControl = exception;
                }
            }
            if (useAccessControl == null) {
                useAccessControl = accessControl.get(DEFAULT);
            }
            return new ResourceAccessControl(useAccessControl);
        }
    }

    static class ResourceAccessControl {
        private final ModelNode accessControl;

        ResourceAccessControl(ModelNode modelNode){
            this.accessControl = modelNode;
        }

        boolean isAccessibleResource() {
            if (accessControl.hasDefined(ADDRESS) && !accessControl.get(ADDRESS).asBoolean()) {
                return false;
            }
            return true;
        }

        public boolean isReadableAttribute(String attribute) {
            ModelNode node = accessControl.get(ATTRIBUTES, attribute, READ);
            if (!node.isDefined()) {
                //Should not happen but return false just in case
                return false;
            }
            return node.asBoolean();
        }

        public boolean isWritableAttribute(String attribute) {
            ModelNode node = accessControl.get(ATTRIBUTES, attribute, WRITE);
            if (!node.isDefined()) {
                //Should not happen but return false just in case
                return false;
            }
            return node.asBoolean();
        }

        public boolean isExecutableOperation(String operation) {
            ModelNode node = accessControl.get(OPERATIONS, operation, EXECUTE);
            if (!node.isDefined()) {
                //Should not happen but return false just in case
                return false;
            }
            return node.asBoolean();
        }
    }
}
