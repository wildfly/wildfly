/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.filters;

import io.undertow.server.handlers.proxy.mod_cluster.ModClusterStatus;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;

/**
 * Runtime representation of a mod_cluster balancer
 *
 * @author Stuart Douglas
 */
class ModClusterBalancerDefinition extends SimpleResourceDefinition {

    public static ModClusterBalancerDefinition INSTANCE = new ModClusterBalancerDefinition();

    public static final AttributeDefinition STICKY_SESSION_COOKIE = new SimpleAttributeDefinitionBuilder(Constants.STICKY_SESSION_COOKIE, ModelType.STRING)
            .setRequired(false)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition STICKY_SESSION_PATH = new SimpleAttributeDefinitionBuilder(Constants.STICKY_SESSION_PATH, ModelType.STRING)
            .setRequired(false)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition MAX_ATTEMPTS = new SimpleAttributeDefinitionBuilder(Constants.MAX_ATTEMPTS, ModelType.INT)
            .setRequired(false)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition WAIT_WORKER = new SimpleAttributeDefinitionBuilder(Constants.WAIT_WORKER, ModelType.INT)
            .setRequired(false)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition STICKY_SESSION = new SimpleAttributeDefinitionBuilder(Constants.STICKY_SESSION, ModelType.BOOLEAN)
            .setRequired(false)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition STICKY_SESSION_FORCE = new SimpleAttributeDefinitionBuilder(Constants.STICKY_SESSION_FORCE, ModelType.BOOLEAN)
            .setRequired(false)
            .setStorageRuntime()
            .build();


    public static final AttributeDefinition STICKY_SESSION_REMOVE = new SimpleAttributeDefinitionBuilder(Constants.STICKY_SESSION_REMOVE, ModelType.BOOLEAN)
            .setRequired(false)
            .setStorageRuntime()
            .build();


    private ModClusterBalancerDefinition() {
        super(new Parameters(UndertowExtension.BALANCER, UndertowExtension.getResolver("handler", "mod-cluster", "balancer"))
                .setRuntime());
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(ModClusterNodeDefinition.INSTANCE);
        resourceRegistration.registerSubModel(ModClusterLoadBalancingGroupDefinition.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(MAX_ATTEMPTS, new AbstractBalancerOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.LoadBalancer ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.getMaxAttempts()));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(STICKY_SESSION_COOKIE, new AbstractBalancerOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.LoadBalancer ctx, ModelNode operation) throws OperationFailedException {
                final String stickySessionCookie = ctx.getStickySessionCookie();
                if(stickySessionCookie == null) {
                    context.getResult().set(new ModelNode());
                } else {
                    context.getResult().set(new ModelNode(stickySessionCookie));
                }
            }
        });

        resourceRegistration.registerReadOnlyAttribute(STICKY_SESSION_PATH, new AbstractBalancerOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.LoadBalancer ctx, ModelNode operation) throws OperationFailedException {
                final String stickySessionPath = ctx.getStickySessionPath();
                if(stickySessionPath == null) {
                    context.getResult().set(new ModelNode());
                } else {
                    context.getResult().set(new ModelNode(stickySessionPath));
                }
            }
        });

        resourceRegistration.registerReadOnlyAttribute(WAIT_WORKER, new AbstractBalancerOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.LoadBalancer ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.getWaitWorker()));
            }
        });

        resourceRegistration.registerReadOnlyAttribute(STICKY_SESSION, new AbstractBalancerOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.LoadBalancer ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.isStickySession()));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(STICKY_SESSION_REMOVE, new AbstractBalancerOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.LoadBalancer ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.isStickySessionRemove()));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(STICKY_SESSION_FORCE, new AbstractBalancerOperation() {

            @Override
            protected void handleNode(OperationContext context, ModClusterStatus.LoadBalancer ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.isStickySessionForce()));
            }
        });
    }


    private abstract class AbstractBalancerOperation implements OperationStepHandler {

        @Override
        public final void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            PathAddress address = context.getCurrentAddress();
            int current = address.size() - 1;
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
            handleNode(context, balancer, operation);
        }

        protected abstract void handleNode(OperationContext context, ModClusterStatus.LoadBalancer ctx, ModelNode operation) throws OperationFailedException ;
    }

}