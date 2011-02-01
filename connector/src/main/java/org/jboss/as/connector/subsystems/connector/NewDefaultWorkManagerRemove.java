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
package org.jboss.as.connector.subsystems.connector;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.connector.subsystems.connector.Constants.*;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
public class NewDefaultWorkManagerRemove implements RuntimeOperationHandler, ModelRemoveOperationHandler {

    static final OperationHandler INSTANCE = new NewDefaultWorkManagerRemove();

    @Override
    public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
        // Apply to the model
        final ModelNode model = context.getSubModel();
        final String name = model.require(NAME).asString();

        if (context instanceof NewRuntimeOperationContext) {

            final NewRuntimeOperationContext runtimeContext = (NewRuntimeOperationContext) context;
            final ServiceController<?> controller = runtimeContext.getServiceRegistry().getService(
                    ConnectorServices.CONNECTOR_CONFIG_SERVICE);
            if (controller == null) {
                resultHandler.handleResultComplete(null);
                return Cancellable.NULL;
            } else {
                // controller.addListener(new
                // UpdateResultHandler.ServiceRemoveListener<P>(handler,
                // param));
                controller.setMode(Mode.REMOVE);
            }
        }

        // Compensating is add
        final ModelNode compensating = new ModelNode();
        compensating.get(OP_ADDR).set(operation.require(ADDRESS));
        compensating.get(OP).set(ADD);
        compensating.get(NAME).set(name);
        if (model.has(ENABLED)) {
            compensating.get(ENABLED).set(model.get(ENABLED));
        }
        if (model.has(FAIL_ON_ERROR)) {
            compensating.get(FAIL_ON_ERROR).set(model.get(FAIL_ON_ERROR));
        }
        if (model.has(FAIL_ON_WARN)) {
            compensating.get(FAIL_ON_WARN).set(model.get(FAIL_ON_WARN));
        }

        resultHandler.handleResultComplete(compensating);

        return Cancellable.NULL;
    }
}
