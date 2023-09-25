/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.BiPredicate;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Adds a given child resource if absent.
 * @author Paul Ferraro
 */
public class AddIfAbsentStepHandler implements OperationStepHandler {

    private final BiPredicate<Resource, PathElement> present;

    public AddIfAbsentStepHandler(BiPredicate<Resource, PathElement> present) {
        this.present = present;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        Resource parentResource = context.readResourceFromRoot(address.getParent(), false);

        if (!this.present.test(parentResource, address.getLastElement())) {
            context.getResourceRegistration().getOperationHandler(PathAddress.EMPTY_ADDRESS, ModelDescriptionConstants.ADD).execute(context, operation);
        }
    }
}