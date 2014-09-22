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

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.JNDI_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.TIMEOUT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
  * Operation handler for quiesce operation
  *
  * @author Stefano Maestri
 */
public class QuiesceHandler implements OperationStepHandler {
    static final QuiesceHandler INSTANCE = new QuiesceHandler();


    public QuiesceHandler() {
        super();
    }

    public void execute(OperationContext context, ModelNode operation) {

        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final ModelNode model = resource.getModel();

        if (context.isNormalServer()) {

            DataSourceStatisticsListener.removeStatisticsResources(resource);

            context.addStep(new OperationStepHandler() {
                public void execute(final OperationContext context, ModelNode operation) throws OperationFailedException {

                    final ModelNode address = operation.require(OP_ADDR);
                    final String dsName = PathAddress.pathAddress(address).getLastElement().getValue();
                    final String jndiName = model.get(JNDI_NAME.getName()).asString();

                    final ServiceRegistry registry = context.getServiceRegistry(true);
                    Integer timeout = TIMEOUT.resolveModelAttribute(context, operation).isDefined() ? TIMEOUT.resolveModelAttribute(context, operation).asInt() : null;
                    final ServiceName dataSourceServiceName = AbstractDataSourceService.SERVICE_NAME_BASE.append(jndiName);
                    final AbstractDataSourceService dataSourceService = (AbstractDataSourceService) registry.getService(dataSourceServiceName).getService();
                    if (dataSourceService != null) {
                        if (timeout != null) {
                            dataSourceService.getDeploymentMD().getConnectionManagers()[0].prepareShutdown(timeout);
                        } else {
                            dataSourceService.getDeploymentMD().getConnectionManagers()[0].prepareShutdown();
                        }

                    } else {
                        throw new OperationFailedException(new ModelNode().set(ConnectorLogger.ROOT_LOGGER.serviceNotAvailable("Data-source", dsName)));
                    }
                    context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                }

            }, OperationContext.Stage.RUNTIME);

        }
        context.stepCompleted();
    }

}
