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
 * Runtime representation of a mod_cluster context
 *
 * @author Stuart Douglas
 */
public class ModClusterContextDefinition extends SimpleResourceDefinition {

    public static ModClusterContextDefinition INSTANCE = new ModClusterContextDefinition();


    public static final AttributeDefinition STATUS = new SimpleAttributeDefinitionBuilder(Constants.STATUS, ModelType.STRING)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition REQUESTS = new SimpleAttributeDefinitionBuilder(Constants.REQUESTS, ModelType.INT)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();


    public final OperationDefinition ENABLE = new SimpleOperationDefinition(Constants.ENABLE, getResourceDescriptionResolver());
    public final OperationDefinition DISABLE = new SimpleOperationDefinition(Constants.DISABLE, getResourceDescriptionResolver());

    ModClusterContextDefinition() {
        super(UndertowExtension.CONTEXT, UndertowExtension.getResolver("handler", "mod-cluster", "balancer", "node", "context"), null, null, true);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadOnlyAttribute(STATUS, new AbstractContextOperation() {

            @Override
            public void handleContext(OperationContext context, ModClusterStatus.Context ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.isEnabled() ? "enabled" : "disabled"));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(REQUESTS, new AbstractContextOperation() {

            @Override
            public void handleContext(OperationContext context, ModClusterStatus.Context ctx, ModelNode operation) throws OperationFailedException {
                context.getResult().set(new ModelNode(ctx.getRequests()));
            }
        });
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerOperationHandler(ENABLE, new AbstractContextOperation() {

            @Override
            protected void handleContext(OperationContext context, ModClusterStatus.Context ctx, ModelNode operation) throws OperationFailedException {
                ctx.enable();
            }
        });
        resourceRegistration.registerOperationHandler(DISABLE, new AbstractContextOperation() {

            @Override
            protected void handleContext(OperationContext context, ModClusterStatus.Context ctx, ModelNode operation) throws OperationFailedException {
                ctx.disable();
            }
        });
    }

    private abstract class AbstractContextOperation implements OperationStepHandler {

        @Override
        public final void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS));
            int current = address.size() - 1;
            String contextName = address.getElement(current--).getValue();
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
            ModClusterStatus.Context ctx = node.getContext(contextName);
            if(ctx == null) {
                context.getResult().set(new ModelNode());
                context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
                return;
            }
            handleContext(context, ctx, operation);
        }

        protected abstract void handleContext(OperationContext context, ModClusterStatus.Context ctx, ModelNode operation) throws OperationFailedException ;
    }

}
