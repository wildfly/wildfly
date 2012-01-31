/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

import org.hornetq.api.core.management.ClusterConnectionControl;
import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handler for runtime operations that interact with a HornetQ {@link org.hornetq.api.core.management.ClusterConnectionControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ClusterConnectionControlHandler extends AbstractHornetQComponentControlHandler<ClusterConnectionControl> {

    public static final ClusterConnectionControlHandler INSTANCE = new ClusterConnectionControlHandler();

    public static final String NODE_ID = "node-id";

    public static final String GET_STATIC_CONNECTORS_AS_JSON = "get-static-connectors-as-json";
    public static final String GET_NODES = "get-nodes";

    private ClusterConnectionControlHandler() {
    }

    @Override
    public void register(ManagementResourceRegistration registry) {
        super.register(registry);

        registry.registerReadOnlyAttribute(NODE_ID, this, AttributeAccess.Storage.RUNTIME);

        final EnumSet<OperationEntry.Flag> flags = EnumSet.of(OperationEntry.Flag.READ_ONLY, OperationEntry.Flag.RUNTIME_ONLY);

        registry.registerOperationHandler(GET_STATIC_CONNECTORS_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, GET_STATIC_CONNECTORS_AS_JSON,
                        CommonAttributes.CLUSTER_CONNECTION, ModelType.STRING, false);
            }
        }, flags);

        registry.registerOperationHandler(GET_NODES, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getGetNodes(locale);
            }
        }, flags);
    }

    @Override
    protected ClusterConnectionControl getHornetQComponentControl(HornetQServer hqServer, PathAddress address) {
        final String resourceName = address.getLastElement().getValue();
        return ClusterConnectionControl.class.cast(hqServer.getManagementService().getResource(ResourceNames.CORE_CLUSTER_CONNECTION + resourceName));
    }

    @Override
    protected String getDescriptionPrefix() {
        return CommonAttributes.CLUSTER_CONNECTION;
    }

    @Override
    protected void handleReadAttribute(String attributeName, OperationContext context, ModelNode operation) throws OperationFailedException {
        if (NODE_ID.equals(attributeName)) {
            ClusterConnectionControl control = getHornetQComponentControl(context, operation, false);
            context.getResult().set(control.getNodeID());
        } else {
            unsupportedAttribute(attributeName);
        }
    }

    @Override
    protected Object handleOperation(String operationName, OperationContext context, ModelNode operation) throws OperationFailedException {
        if (GET_STATIC_CONNECTORS_AS_JSON.equals(operationName)) {
            ClusterConnectionControl control = getHornetQComponentControl(context, operation, false);
            try {
                context.getResult().set(control.getStaticConnectorsAsJSON());
            } catch (Exception e) {
                context.getFailureDescription().set(e.getLocalizedMessage());
            }
        } else if (GET_NODES.equals(operationName)) {
            ClusterConnectionControl control = getHornetQComponentControl(context, operation, false);
            try {
                Map<String, String> nodes = control.getNodes();
                final ModelNode result = context.getResult();
                result.setEmptyObject();
                for (Map.Entry<String, String> entry : nodes.entrySet()) {
                    result.get(entry.getKey()).set(entry.getValue());
                }
            } catch (Exception e) {
                context.getFailureDescription().set(e.getLocalizedMessage());
            }
        } else {
            unsupportedOperation(operationName);
        }

        return null;
    }
}
