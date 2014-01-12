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

package org.jboss.as.cli.accesscontrol;

import java.util.Iterator;

import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestAddress.Node;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * @author Alexey Loubyansky
 *
 */
public class CLIAccessControl {

    private static final Logger log = Logger.getLogger(CLIAccessControl.class);

    public static boolean isExecute(ModelControllerClient client, OperationRequestAddress address, String operation) {
        return isExecute(client, null, address, operation);
    }

    public static boolean isExecute(ModelControllerClient client, String[] parent, OperationRequestAddress address, String operation) {
        final ModelNode accessControl = getAccessControl(client, parent, address, true);
        if(accessControl == null) {
            return false;
        }

        if(!accessControl.has(Util.DEFAULT)) {
            log.warn("access-control is missing defaults: " + accessControl);
            return false;
        }

        final ModelNode defaults = accessControl.get(Util.DEFAULT);
        if(!defaults.has(Util.OPERATIONS)) {
            log.warn("access-control/default is missing operations: " + defaults);
            return false;
        }

        final ModelNode operations = defaults.get(Util.OPERATIONS);
        if(!operations.has(operation)) {
            if(log.isTraceEnabled()) {
                log.trace("The operation is missing in the description: " + operation);
            }
            return false;
        }

        final ModelNode opAC = operations.get(operation);
        if(!opAC.has(Util.EXECUTE)) {
            log.warn("'execute' is missing for " + operation + " in " + accessControl);
            return false;
        }

        return opAC.get(Util.EXECUTE).asBoolean();
    }

    /**
     * Executed read-resource-description and returns access-control info.
     * Returns null in case there was any kind of problem.
     *
     * @param client
     * @param address
     * @return
     */
    public static ModelNode getAccessControl(ModelControllerClient client, OperationRequestAddress address, boolean operations) {
        return getAccessControl(client, null, address, operations);
    }

    public static ModelNode getAccessControl(ModelControllerClient client, String[] parent, OperationRequestAddress address, boolean operations) {

        if(client == null) {
            return null;
        }

        if(address.endsOnType()) {
            log.debug("The prefix ends on a type.");
            return null;
        }

        final ModelNode request = new ModelNode();
        setAddress(request, parent, address);
        request.get(Util.OPERATION).set(Util.READ_RESOURCE_DESCRIPTION);
        request.get(Util.ACCESS_CONTROL).set(Util.TRIM_DESCRIPTIONS);
        if(operations) {
            request.get(Util.OPERATIONS).set(true);
        }

        final ModelNode response;
        try {
            response = client.execute(request);
        } catch (Exception e) {
            log.warn("Failed to execute " + Util.READ_RESOURCE_DESCRIPTION, e);
            return null;
        }

        if (!Util.isSuccess(response)) {
            log.debug("Failed to execute " + Util.READ_RESOURCE_DESCRIPTION + ": " + response);
            return null;
        }

        if(!response.has(Util.RESULT)) {
            log.warn("Response is missing result for " + Util.READ_RESOURCE_DESCRIPTION + ": " + response);
            return null;
        }

        final ModelNode result = response.get(Util.RESULT);
        if(!result.has(Util.ACCESS_CONTROL)) {
            log.warn("Result is missing access-control for " + Util.READ_RESOURCE_DESCRIPTION + ": " + response);
            return null;
        }

        return result.get(Util.ACCESS_CONTROL);
    }

    private static void setAddress(ModelNode request, String[] parent, OperationRequestAddress address) {
        final ModelNode addressNode = request.get(Util.ADDRESS);
        addressNode.setEmptyList();
        if(parent != null) {
            int i = 0;
            while(i < parent.length) {
                addressNode.add(parent[i++], parent[i++]);
            }
        }

        if(!address.isEmpty()) {
            final Iterator<Node> iterator = address.iterator();
            while (iterator.hasNext()) {
                final OperationRequestAddress.Node node = iterator.next();
                if (node.getName() != null) {
                    addressNode.add(node.getType(), node.getName());
                } else if (iterator.hasNext()) {
                    throw new IllegalArgumentException(
                            "The node name is not specified for type '"
                                    + node.getType() + "'");
                }
            }
        }
    }
}
