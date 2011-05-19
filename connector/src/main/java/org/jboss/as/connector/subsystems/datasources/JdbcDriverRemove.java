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

import static org.jboss.as.connector.subsystems.datasources.Constants.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Operation handler responsible for removing a jdbc driver.
 * @author John Bailey
 */
public class JdbcDriverRemove implements ModelRemoveOperationHandler {
    static final JdbcDriverRemove INSTANCE = new JdbcDriverRemove();

    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler)
            throws OperationFailedException {

        final ModelNode opAddr = operation.require(OP_ADDR);

        // Compensating is add
        final ModelNode model = context.getSubModel();
        final ModelNode compensating = Util.getEmptyOperation(ADD, opAddr);
        final String driverName = model.get(DRIVER_NAME).asString();
        compensating.get(DRIVER_NAME).set(model.get(DRIVER_NAME));
        compensating.get(DRIVER_MODULE_NAME).set(model.get(DRIVER_MODULE_NAME));
        compensating.get(DRIVER_MAJOR_VERSION).set(model.get(DRIVER_MAJOR_VERSION));
        compensating.get(DRIVER_MINOR_VERSION).set(model.get(DRIVER_MINOR_VERSION));
        compensating.get(DRIVER_CLASS_NAME).set(model.get(DRIVER_CLASS_NAME));
        compensating.get(DRIVER_XA_DATASOURCE_CLASS_NAME).set(model.get(DRIVER_XA_DATASOURCE_CLASS_NAME));

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(final RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceRegistry registry = context.getServiceRegistry();

                    // Use the module for now. Would be nice to keep the driver
                    // info in the model.
                    final ServiceName serviceName = ServiceName.JBOSS.append("jdbc-driver", driverName);
                    final ServiceController<?> controller = registry.getService(serviceName);
                    if (controller != null) {
                        controller.setMode(ServiceController.Mode.REMOVE);
                    }
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensating);
    }
}
