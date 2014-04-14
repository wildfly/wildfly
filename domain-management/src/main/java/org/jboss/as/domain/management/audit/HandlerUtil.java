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
package org.jboss.as.domain.management.audit;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CUSTOM_FORMATTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE_HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JSON_FORMATTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSLOG_HANDLER;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.logging.DomainManagementLogger;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class HandlerUtil {

    private static final String[] HANDLER_TYPES = new String[] {SYSLOG_HANDLER, FILE_HANDLER};
    private static final String[] FORMATTER_TYPES = new String[]{JSON_FORMATTER, CUSTOM_FORMATTER};

    static void checkNoOtherHandlerWithTheSameName(OperationContext context, ModelNode addHandlerOp) throws OperationFailedException {
        PathAddress addr = findExistingSiblingWithSameName(context, addHandlerOp, HANDLER_TYPES);
        if (addr != null) {
            throw DomainManagementLogger.ROOT_LOGGER.handlerAlreadyExists(addr.getLastElement().getValue(), addr);
        }
    }


    static void checkNoOtherFormatterWithTheSameName(OperationContext context, ModelNode addFormatterOp) throws OperationFailedException {
        PathAddress addr = findExistingSiblingWithSameName(context, addFormatterOp, FORMATTER_TYPES);
        if (addr != null) {
            throw DomainManagementLogger.ROOT_LOGGER.formatterAlreadyExists(addr.getLastElement().getValue(), addr);
        }
    }

    private static PathAddress findExistingSiblingWithSameName(OperationContext context, ModelNode addOp, String[] types) {
        PathAddress address = PathAddress.pathAddress(addOp.require(OP_ADDR));
        PathAddress parentAddress = address.subAddress(0, address.size() - 1);
        Resource resource = context.readResourceFromRoot(parentAddress);

        PathElement element = address.getLastElement();

        for (String type : types) {
            if (!type.equals(element.getKey())) {
                PathElement check = PathElement.pathElement(type, element.getValue());
                if (resource.hasChild(check)){
                    return parentAddress.append(check);
                }
            }
        }
        return null;
    }


    static boolean lookForFormatter(OperationContext context, PathAddress addr, String name) {
        final Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS);
        PathAddress baseAddr = addr.subAddress(0, addr.size() - 1);
        for (String type : FORMATTER_TYPES) {
            PathAddress referenceAddress = baseAddr.append(type, name);
            if (lookForResource(root, referenceAddress)){
                return true;
            }
        }
        return false;
    }

    private static boolean lookForResource(final Resource root, final PathAddress pathAddress) {
        Resource current = root;
        for (PathElement element : pathAddress) {
            current = current.getChild(element);
            if (current == null) {
                return false;
            }
        }
        return true;
    }
}
