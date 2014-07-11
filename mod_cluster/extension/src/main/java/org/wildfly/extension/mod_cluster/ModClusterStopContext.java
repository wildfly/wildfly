/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.mod_cluster;

import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.modcluster.ModClusterServiceMBean;
import org.jboss.msc.service.ServiceController;

import static org.wildfly.extension.mod_cluster.ModClusterLogger.ROOT_LOGGER;

public class ModClusterStopContext implements OperationStepHandler {

    static final ModClusterStopContext INSTANCE = new ModClusterStopContext();

    static OperationDefinition getDefinition(ResourceDescriptionResolver descriptionResolver) {
        return new SimpleOperationDefinitionBuilder(CommonAttributes.STOP_CONTEXT, descriptionResolver)
                .addParameter(ModClusterSubsystemResourceDefinition.VIRTUAL_HOST)
                .addParameter(ModClusterSubsystemResourceDefinition.CONTEXT)
                .addParameter(ModClusterSubsystemResourceDefinition.WAIT_TIME)
                .setRuntimeOnly()
                .build();
    }

    @Override
    public void execute(OperationContext context, ModelNode operation)
            throws OperationFailedException {
        if (context.isNormalServer() && context.getServiceRegistry(false).getService(ContainerEventHandlerService.SERVICE_NAME) != null) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    ServiceController<?> controller = context.getServiceRegistry(false).getService(ContainerEventHandlerService.SERVICE_NAME);
                    final ModClusterServiceMBean service = (ModClusterServiceMBean) controller.getValue();
                    ROOT_LOGGER.debugf("stop-context: %s", operation);
                    final ContextHost contexthost = new ContextHost(operation);

                    try {
                        service.stopContext(contexthost.webhost, contexthost.webcontext, contexthost.waittime, TimeUnit.SECONDS);
                    } catch(IllegalArgumentException e) {
                        throw new OperationFailedException(new ModelNode().set(ModClusterLogger.ROOT_LOGGER.ContextOrHostNotFound(contexthost.webhost, contexthost.webcontext)));
                    }

                    context.completeStep(new OperationContext.RollbackHandler() {
                        @Override
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            // TODO We're assuming that the context was previously enabled, but it could have been disabled
                            service.enableContext(contexthost.webhost, contexthost.webcontext);
                        }
                    });
                }
            }, OperationContext.Stage.RUNTIME);
        }

        context.stepCompleted();
    }
}
