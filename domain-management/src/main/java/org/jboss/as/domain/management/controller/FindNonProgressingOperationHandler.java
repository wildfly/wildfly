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
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.domain.management.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXCLUSIVE_RUNNING_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_OPERATIONS;

import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.logging.DomainManagementLogger;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.OperationStepHandler} that looks for and returns the id of single operation that is in
 * execution status {@link org.jboss.as.controller.OperationContext.ExecutionStatus#AWAITING_STABILITY}
 * and has been executing in that status for longer than a specified {@code timeout} seconds.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class FindNonProgressingOperationHandler implements OperationStepHandler {

    private static final AttributeDefinition STABILITY_TIMEOUT = SimpleAttributeDefinitionBuilder.create("timeout", ModelType.INT, false)
            .setDefaultValue(new ModelNode(15))
            .setValidator(new IntRangeValidator(0, true))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .build();

    static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder("find-non-progressing-operation",
            DomainManagementResolver.getResolver(CORE, MANAGEMENT_OPERATIONS))
            .setReplyType(ModelType.STRING)
            .withFlag(OperationEntry.Flag.HOST_CONTROLLER_ONLY)
            .build();

    static final OperationStepHandler INSTANCE = new FindNonProgressingOperationHandler();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final long timeout = TimeUnit.SECONDS.toNanos(STABILITY_TIMEOUT.resolveModelAttribute(context, operation).asLong());

        DomainManagementLogger.ROOT_LOGGER.debugf("Identification of operation not progressing after [%d] ns has been requested", timeout);

        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        ModelNode result = context.getResult();
        for (Resource.ResourceEntry child : resource.getChildren(ModelDescriptionConstants.ACTIVE_OPERATION)) {
            ModelNode model = child.getModel();
            if (model.get(EXCLUSIVE_RUNNING_TIME).asLong() > timeout) {
                result.set(child.getName());
                break;
            }
        }
        context.stepCompleted();
    }
}
