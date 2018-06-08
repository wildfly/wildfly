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

import static org.wildfly.extension.mod_cluster.ModClusterLogger.ROOT_LOGGER;
import static org.wildfly.extension.mod_cluster.ModClusterSubsystemResourceDefinition.WAIT_TIME;

import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.modcluster.ModClusterServiceMBean;
import org.wildfly.clustering.service.ActiveServiceSupplier;

/**
 * {@link OperationStepHandler} that attempts to gracefully stop all web applications, within the specified timeout.
 *
 * @author Jean-Frederic Clere
 * @author Radoslav Husar
 */
public class ModClusterStop implements OperationStepHandler {

    static final ModClusterStop INSTANCE = new ModClusterStop();

    static OperationDefinition getDefinition(ResourceDescriptionResolver descriptionResolver) {
        return new SimpleOperationDefinitionBuilder(CommonAttributes.STOP, descriptionResolver)
                .addParameter(WAIT_TIME)
                .setRuntimeOnly()
                .build();
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (context.isNormalServer() && context.getServiceRegistry(false).getService(ContainerEventHandlerServiceConfigurator.SERVICE_NAME) != null) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    ModClusterServiceMBean service = new ActiveServiceSupplier<ModClusterServiceMBean>(context.getServiceRegistry(true), ContainerEventHandlerServiceConfigurator.SERVICE_NAME).get();
                    ROOT_LOGGER.debugf("stop: %s", operation);

                    final int waitTime = WAIT_TIME.resolveModelAttribute(context, operation).asInt();

                    boolean success = service.stop(waitTime, TimeUnit.SECONDS);
                    context.getResult().get(CommonAttributes.SESSION_DRAINING_COMPLETE).set(success);

                    context.completeStep(new OperationContext.RollbackHandler() {
                        @Override
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            // TODO We're assuming that the all contexts were previously enabled, but they could have been disabled
                            service.enable();
                        }
                    });
                }
            }, OperationContext.Stage.RUNTIME);
        }
   }
}
