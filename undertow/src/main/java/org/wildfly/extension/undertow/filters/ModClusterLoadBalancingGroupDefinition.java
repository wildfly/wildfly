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
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;

/**
 * Runtime representation of a mod_cluster context
 *
 * @author Stuart Douglas
 */
public class ModClusterLoadBalancingGroupDefinition extends SimpleResourceDefinition {

    public static ModClusterLoadBalancingGroupDefinition INSTANCE = new ModClusterLoadBalancingGroupDefinition();


    public final OperationDefinition ENABLE_NODES = new SimpleOperationDefinition(Constants.ENABLE_NODES, getResourceDescriptionResolver());
    public final OperationDefinition DISABLE_NODES = new SimpleOperationDefinition(Constants.DISABLE_NODES, getResourceDescriptionResolver());
    public final OperationDefinition STOP_NODES = new SimpleOperationDefinition(Constants.STOP_NODES, getResourceDescriptionResolver());

    ModClusterLoadBalancingGroupDefinition() {
        super(new Parameters(UndertowExtension.LOAD_BALANCING_GROUP, UndertowExtension.getResolver("handler", "mod-cluster", "balancer", "load-balancing-group"))
                .setRuntime());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerOperationHandler(ENABLE_NODES, new AbstractGroupOperation() {

            @Override
            protected void handleGroup(OperationContext context, ModClusterStatus.LoadBalancer balancer, String groupName, ModelNode operation) {
                for(ModClusterStatus.Node node : balancer.getNodes()) {
                    if(groupName.equals(node.getDomain())) {
                        for(ModClusterStatus.Context n : node.getContexts()) {
                            n.enable();
                        }
                    }
                }
            }
        });
        resourceRegistration.registerOperationHandler(DISABLE_NODES, new AbstractGroupOperation() {

            @Override
            protected void handleGroup(OperationContext context, ModClusterStatus.LoadBalancer balancer, String groupName, ModelNode operation) {
                for(ModClusterStatus.Node node : balancer.getNodes()) {
                    if(groupName.equals(node.getDomain())) {
                        for(ModClusterStatus.Context n : node.getContexts()) {
                            n.disable();
                        }
                    }
                }
            }
        });
        resourceRegistration.registerOperationHandler(STOP_NODES, new AbstractGroupOperation() {

            @Override
            protected void handleGroup(OperationContext context, ModClusterStatus.LoadBalancer balancer, String groupName, ModelNode operation) {
                for(ModClusterStatus.Node node : balancer.getNodes()) {
                    if(groupName.equals(node.getDomain())) {
                        for(ModClusterStatus.Context n : node.getContexts()) {
                            n.stop();
                        }
                    }
                }
            }
        });
    }

    private abstract class AbstractGroupOperation implements OperationStepHandler {

        @Override
        public final void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS));
            int current = address.size() - 1;
            String groupName = address.getElement(current--).getValue();
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
            handleGroup(context, balancer, groupName, operation);
        }

        protected abstract void handleGroup(OperationContext context, ModClusterStatus.LoadBalancer balancer, String groupName, ModelNode operation);

    }
}
