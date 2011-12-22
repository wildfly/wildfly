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

package org.jboss.as.domain.controller.operations.coordination;

import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_FAILURE_DESCRIPTIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import org.jboss.dmr.ModelNode;

/**
 * Assembles the overall result for a domain operation from individual host and server results.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DomainFailureResultHandler implements OperationStepHandler {

    private final DomainOperationContext domainOperationContext;

    public DomainFailureResultHandler(DomainOperationContext domainOperationContext) {
        this.domainOperationContext = domainOperationContext;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        // Wipe out any result that may have accumulated from previous handlers
        context.getResult().set(new ModelNode());

        final boolean isDomain = isDomainOperation(operation);
        if (!collectDomainFailure(context, isDomain)) {
            collectHostFailures(context, isDomain);
        }

        context.completeStep();
    }

    private boolean collectDomainFailure(OperationContext context, final boolean isDomain) {
        final ModelNode coordinator = domainOperationContext.getCoordinatorResult();
        ModelNode domainFailure = null;
        if (isDomain &&  coordinator != null && coordinator.has(FAILURE_DESCRIPTION)) {
            domainFailure = coordinator.hasDefined(FAILURE_DESCRIPTION) ? coordinator.get(FAILURE_DESCRIPTION) : new ModelNode().set(MESSAGES.unexplainedFailure());
        }
        if (domainFailure != null) {
            context.getFailureDescription().get(DOMAIN_FAILURE_DESCRIPTION).set(domainFailure);
            return true;
        }
        return false;
    }

    private boolean collectHostFailures(final OperationContext context, final boolean isDomain) {
        ModelNode hostFailureResults = null;
        for (Map.Entry<String, ModelNode> entry : domainOperationContext.getHostControllerResults().entrySet()) {
            ModelNode hostResult = entry.getValue();
            if (hostResult.has(FAILURE_DESCRIPTION)) {
                if (hostFailureResults == null) {
                    hostFailureResults = new ModelNode();
                }
                final ModelNode desc = hostResult.hasDefined(FAILURE_DESCRIPTION) ? hostResult.get(FAILURE_DESCRIPTION) : new ModelNode().set(MESSAGES.unexplainedFailure());
                hostFailureResults.add(entry.getKey(), desc);
            }
        }

        final ModelNode coordinator = domainOperationContext.getCoordinatorResult();
        if (!isDomain && coordinator != null && coordinator.has(FAILURE_DESCRIPTION)) {
            if (hostFailureResults == null) {
                hostFailureResults = new ModelNode();
            }
            final ModelNode desc = coordinator.hasDefined(FAILURE_DESCRIPTION) ? coordinator.get(FAILURE_DESCRIPTION) : new ModelNode().set(MESSAGES.unexplainedFailure());
            hostFailureResults.add(domainOperationContext.getLocalHostInfo().getLocalHostName(), desc);
        }

        if (hostFailureResults != null) {
            context.getFailureDescription().get(HOST_FAILURE_DESCRIPTIONS).set(hostFailureResults);
            return true;
        }
        return false;
    }

    private boolean isDomainOperation(final ModelNode operation) {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        return address.size() == 0 || !address.getElement(0).getKey().equals(HOST);
    }
}
