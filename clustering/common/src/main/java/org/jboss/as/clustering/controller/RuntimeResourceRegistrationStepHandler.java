/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.dmr.ModelNode;

/**
 * {@link OperationStepHandler} that registers runtime resources.
 * @author Paul Ferraro
 */
public class RuntimeResourceRegistrationStepHandler implements OperationStepHandler {

    private final RuntimeResourceRegistration registration;

    public RuntimeResourceRegistrationStepHandler(RuntimeResourceRegistration registration) {
        this.registration = registration;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        this.registration.register(context);
    }
}
