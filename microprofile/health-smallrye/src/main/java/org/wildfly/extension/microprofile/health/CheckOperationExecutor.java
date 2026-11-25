/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.health;

import java.util.function.Supplier;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.resource.executor.RuntimeOperation;
import org.wildfly.subsystem.resource.executor.RuntimeOperationExecutor;

/**
 * Executes MicroProfile health runtime operations.
 * @author Paul Ferraro
 */
class CheckOperationExecutor implements RuntimeOperationExecutor<MicroProfileHealthReporter> {
    private final Supplier<MicroProfileHealthReporter> reporter;

    CheckOperationExecutor(Supplier<MicroProfileHealthReporter> reporter) {
        this.reporter = reporter;
    }

    @Override
    public ModelNode execute(OperationContext context, ModelNode operation, RuntimeOperation<MicroProfileHealthReporter> executable) throws OperationFailedException {
        MicroProfileHealthReporter reporter = this.reporter.get();
        return (reporter != null) ? executable.execute(context, operation, reporter) : null;
    }
}
