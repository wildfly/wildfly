/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.undertow.filters;

import io.undertow.server.handlers.proxy.mod_cluster.ModClusterStatus;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;

/**
 * Runtime representation of a mod_cluster node
 *
 * @author Stuart Douglas
 */
public class ModClusterNodeDefinition extends SimpleResourceDefinition {

    public static ModClusterNodeDefinition INSTANCE = new ModClusterNodeDefinition();


    public static final AttributeDefinition LOAD = new SimpleAttributeDefinitionBuilder(Constants.LOAD, ModelType.INT)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition STATUS = new SimpleAttributeDefinitionBuilder(Constants.STATUS, ModelType.STRING)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition LOAD_BALANCING_GROUP = new SimpleAttributeDefinitionBuilder(Constants.LOAD_BALANCING_GROUP, ModelType.STRING)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition CACHE_CONNECTIONS = new SimpleAttributeDefinitionBuilder(Constants.CACHE_CONNECTIONS, ModelType.INT)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition MAX_CONNECTIONS = new SimpleAttributeDefinitionBuilder(Constants.MAX_CONNECTIONS, ModelType.INT)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition OPEN_CONNECTIONS = new SimpleAttributeDefinitionBuilder(Constants.OPEN_CONNECTIONS, ModelType.INT)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition PING = new SimpleAttributeDefinitionBuilder(Constants.PING, ModelType.INT)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition READ = new SimpleAttributeDefinitionBuilder(Constants.READ, ModelType.LONG)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition REQUEST_QUEUE_SIZE = new SimpleAttributeDefinitionBuilder(Constants.REQUEST_QUEUE_SIZE, ModelType.INT)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition TIMEOUT = new SimpleAttributeDefinitionBuilder(Constants.TIMEOUT, ModelType.INT)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition WRITTEN = new SimpleAttributeDefinitionBuilder(Constants.WRITTEN, ModelType.LONG)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition TTL = new SimpleAttributeDefinitionBuilder(Constants.TTL, ModelType.LONG)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();


    public static final AttributeDefinition FLUSH_PACKETS = new SimpleAttributeDefinitionBuilder(Constants.FLUSH_PACKETS, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition QUEUE_NEW_REQUESTS = new SimpleAttributeDefinitionBuilder(Constants.QUEUE_NEW_REQUESTS, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();

    public final OperationDefinition ENABLE = new SimpleOperationDefinition(Constants.ENABLE, getResourceDescriptionResolver());
    public final OperationDefinition DISABLE = new SimpleOperationDefinition(Constants.DISABLE, getResourceDescriptionResolver());


    ModClusterNodeDefinition() {
        super(UndertowExtension.NODE, UndertowExtension.getResolver("handler","mod-cluster", "balancer", "node"), null, null, true);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(ModClusterContextDefinition.INSTANCE);
    }


    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerOperationHandler(ENABLE, new AbstractNodeOperation() {
            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.Node ctx, ModelNode operation) throws OperationFailedException {
                for(ModClusterStatus.Context n : ctx.getContexts()) {
                    n.enable();
                }
            }
        });
        resourceRegistration.registerOperationHandler(DISABLE, new AbstractNodeOperation() {
            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.Node ctx, ModelNode operation) throws OperationFailedException {
                for(ModClusterStatus.Context n : ctx.getContexts()) {
                    n.disable();
                }
            }
        });
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(LOAD, new AbstractNodeOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.Node ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.getLoad()));
            }
        });

        resourceRegistration.registerReadOnlyAttribute(STATUS, new AbstractNodeOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.Node ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.getStatus().name()));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(LOAD_BALANCING_GROUP, new AbstractNodeOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.Node ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.getDomain()));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(CACHE_CONNECTIONS, new AbstractNodeOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.Node ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.getCacheConnections()));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(MAX_CONNECTIONS, new AbstractNodeOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.Node ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.getMaxConnections()));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(OPEN_CONNECTIONS, new AbstractNodeOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.Node ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.getOpenConnections()));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(PING, new AbstractNodeOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.Node ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.getPing()));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(READ, new AbstractNodeOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.Node ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.getRead()));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(REQUEST_QUEUE_SIZE, new AbstractNodeOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.Node ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.getRequestQueueSize()));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(TIMEOUT, new AbstractNodeOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.Node ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.getTimeout()));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(WRITTEN, new AbstractNodeOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.Node ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.getTransferred()));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(TTL, new AbstractNodeOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.Node ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.getTtl()));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(FLUSH_PACKETS, new AbstractNodeOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.Node ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.isFlushPackets()));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(QUEUE_NEW_REQUESTS, new AbstractNodeOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.Node ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.isQueueNewRequests()));
            }
        });
    }

    private abstract class AbstractNodeOperation implements OperationStepHandler {

        @Override
        public final void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS));
            int current = address.size() - 1;
            String nodeName = address.getElement(current--).getValue();
            String balancerName = address.getElement(current--).getValue();
            String modClusterName = address.getElement(current--).getValue();
            ModClusterService service = ModClusterResource.service(modClusterName);
            if (service == null) {
                context.getResult().set(new ModelNode());
                context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
                return;
            }
            ModClusterStatus.LoadBalancer balancer = service.getModCluster().getController().getStatus().getLoadBalancer(balancerName);
            if (balancer == null) {
                context.getResult().set(new ModelNode());
                context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
                return;
            }
            ModClusterStatus.Node node = balancer.getNode(nodeName);
            if (node == null) {
                context.getResult().set(new ModelNode());
                context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
                return;
            }
            handleNode(context, node, operation);
        }

        protected abstract void handleNode(OperationContext context, ModClusterStatus.Node ctx, ModelNode operation) throws OperationFailedException ;
    }

}
