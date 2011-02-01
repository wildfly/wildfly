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

import static org.jboss.as.connector.subsystems.connector.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.connector.Constants.FAIL_ON_ERROR;
import static org.jboss.as.connector.subsystems.connector.Constants.FAIL_ON_WARN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.bootstrap.CloneableBootstrapContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
public class NewArchiveValidationAdd implements RuntimeOperationHandler, ModelAddOperationHandler {

    static final OperationHandler INSTANCE = new NewArchiveValidationAdd();

    @Override
    public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        boolean archiveValidationEnabled = NewParamsUtils.parseBooleanParameter(operation, ENABLED, false);
        boolean failOnError = NewParamsUtils.parseBooleanParameter(operation, FAIL_ON_ERROR, true);
        boolean failOnWarn = NewParamsUtils.parseBooleanParameter(operation, FAIL_ON_WARN, false);

        if (context instanceof NewRuntimeOperationContext) {
            ServiceTarget target = ((NewRuntimeOperationContext) context).getServiceTarget();
            final ConnectorSubsystemConfiguration config = new ConnectorSubsystemConfiguration();

            config.setArchiveValidation(archiveValidationEnabled);
            config.setArchiveValidationFailOnError(failOnError);
            config.setArchiveValidationFailOnWarn(failOnWarn);

            final ConnectorConfigService connectorConfigService = new ConnectorConfigService(config);

            // TODO add the handoffExceutor injection

            final ServiceBuilder<ConnectorSubsystemConfiguration> serviceBuilder = target
                    .addService(ConnectorServices.CONNECTOR_CONFIG_SERVICE, connectorConfigService)
                    .addDependency(ConnectorServices.DEFAULT_BOOTSTRAP_CONTEXT_SERVICE, CloneableBootstrapContext.class,
                            connectorConfigService.getDefaultBootstrapContextInjector()).setInitialMode(Mode.ACTIVE);

            serviceBuilder.install();

        }

        // Apply to the model, but don't set default. Only realy defined params
        // in operation should be considered
        final ModelNode model = context.getSubModel();
        if (NewParamsUtils.has(operation, ENABLED)) {
            model.get(ENABLED).set(archiveValidationEnabled);
        }
        if (NewParamsUtils.has(operation, FAIL_ON_ERROR)) {
            model.get(FAIL_ON_ERROR).set(failOnError);
        }
        if (NewParamsUtils.has(operation, FAIL_ON_WARN)) {
            model.get(FAIL_ON_WARN).set(failOnWarn);
        }

        // Compensating is remove
        final ModelNode compensating = new ModelNode();
        compensating.get(OP_ADDR).set(operation.require(ADDRESS));
        compensating.get(OP).set(REMOVE);
        resultHandler.handleResultComplete(compensating);

        return Cancellable.NULL;
    }
}
