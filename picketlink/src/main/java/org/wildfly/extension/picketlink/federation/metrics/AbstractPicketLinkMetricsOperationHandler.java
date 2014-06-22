/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.federation.metrics;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.service.PicketLinkFederationService;
import org.wildfly.extension.picketlink.logging.PicketLinkLogger;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public abstract class AbstractPicketLinkMetricsOperationHandler implements OperationStepHandler {

    protected static final SimpleAttributeDefinition ERROR_RESPONSE_TO_SP_COUNT = new SimpleAttributeDefinitionBuilder(ModelElement.METRICS_ERROR_RESPONSE_TO_SP_COUNT.getName(), ModelType.INT, true)
        .setStorageRuntime()
        .build();
    protected static final SimpleAttributeDefinition ERROR_SIGN_VALIDATION_COUNT = new SimpleAttributeDefinitionBuilder(ModelElement.METRICS_ERROR_SIGN_VALIDATION_COUNT.getName(), ModelType.INT, true)
        .setStorageRuntime()
        .build();
    protected static final SimpleAttributeDefinition ERROR_TRUSTED_DOMAIN_COUNT = new SimpleAttributeDefinitionBuilder(ModelElement.METRICS_ERROR_TRUSTED_DOMAIN_COUNT.getName(), ModelType.INT, true)
        .setStorageRuntime()
        .build();
    protected static final SimpleAttributeDefinition EXPIRED_ASSERTIONS_COUNT = new SimpleAttributeDefinitionBuilder(ModelElement.METRICS_EXPIRED_ASSERTIONS_COUNT.getName(), ModelType.INT, true)
        .setStorageRuntime()
        .build();
    protected static final SimpleAttributeDefinition LOGIN_INIT_COUNT = new SimpleAttributeDefinitionBuilder(ModelElement.METRICS_LOGIN_INIT_COUNT.getName(), ModelType.INT, true)
        .setStorageRuntime()
        .build();
    protected static final SimpleAttributeDefinition LOGIN_COMPLETE_COUNT = new SimpleAttributeDefinitionBuilder(ModelElement.METRICS_LOGIN_COMPLETE_COUNT.getName(), ModelType.INT, true)
        .setStorageRuntime()
        .build();
    protected static final SimpleAttributeDefinition REQUEST_FROM_IDP_COUNT = new SimpleAttributeDefinitionBuilder(ModelElement.METRICS_REQUEST_FROM_IDP_COUNT.getName(), ModelType.INT, true)
        .setStorageRuntime()
        .build();
    protected static final SimpleAttributeDefinition RESPONSE_FROM_IDP_COUNT = new SimpleAttributeDefinitionBuilder(ModelElement.METRICS_RESPONSE_FROM_IDP_COUNT.getName(), ModelType.INT, true)
        .setStorageRuntime()
        .build();
    protected static final SimpleAttributeDefinition REQUEST_TO_IDP_COUNT = new SimpleAttributeDefinitionBuilder(ModelElement.METRICS_REQUEST_TO_IDP_COUNT.getName(), ModelType.INT, true)
        .setStorageRuntime()
        .build();
    protected static final SimpleAttributeDefinition CREATED_ASSERTIONS_COUNT = new SimpleAttributeDefinitionBuilder(ModelElement.METRICS_CREATED_ASSERTIONS_COUNT.getName(), ModelType.INT, true)
        .setStorageRuntime()
        .build();
    protected static final SimpleAttributeDefinition RESPONSE_TO_SP_COUNT = new SimpleAttributeDefinitionBuilder(ModelElement.METRICS_RESPONSE_TO_SP_COUNT.getName(), ModelType.INT, true)
        .setStorageRuntime()
        .build();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
                final String name = address.getLastElement().getValue();
                final String attributeName = operation.require(NAME).asString();
                final ServiceController<?> controller = context.getServiceRegistry(false)
                    .getRequiredService(createServiceName(name));

                try {
                    PicketLinkFederationService<?> service = (PicketLinkFederationService<?>) controller.getValue();

                    doPopulateResult(service.getMetrics(), context.getResult(), attributeName);
                } catch (Exception e) {
                    throw PicketLinkLogger.ROOT_LOGGER.failedToGetMetrics(e.getMessage());
                }

                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    protected void doPopulateResult(PicketLinkSubsystemMetrics metrics, ModelNode result, String attributeName) {
    }

    protected abstract ServiceName createServiceName(String name);
}
