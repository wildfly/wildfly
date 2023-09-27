/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.health;

import static org.wildfly.extension.health._private.HealthLogger.LOGGER;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class HealthSubsystemAdd extends AbstractBoottimeAddStepHandler {

    HealthSubsystemAdd() {
        super(HealthSubsystemDefinition.ATTRIBUTES);
    }

    static final HealthSubsystemAdd INSTANCE = new HealthSubsystemAdd();

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performBoottime(context, operation, model);

        final boolean securityEnabled = HealthSubsystemDefinition.SECURITY_ENABLED.resolveModelAttribute(context, model).asBoolean();

        HealthContextService.install(context, securityEnabled);
        ServerProbesService.install(context);

        LOGGER.activatingSubsystem();
    }
}