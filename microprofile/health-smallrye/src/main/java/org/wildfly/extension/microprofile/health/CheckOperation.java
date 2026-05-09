/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.health;

import java.util.function.Function;

import io.smallrye.health.SmallRyeHealth;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.executor.RuntimeOperation;

/**
 * Management operations that return the DMR representation of the MicroProfile Health Check JSON payload.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 * @author Paul Ferraro
 */
enum CheckOperation implements RuntimeOperation<MicroProfileHealthReporter> {

    CHECK("check", MicroProfileHealthReporter::getHealth),
    CHECK_LIVE("check-live", MicroProfileHealthReporter::getLiveness),
    CHECK_READY("check-ready", MicroProfileHealthReporter::getReadiness),
    CHECK_STARTED("check-started", MicroProfileHealthReporter::getStartup),
    ;

    private final OperationDefinition definition;
    private final Function<MicroProfileHealthReporter, SmallRyeHealth> operator;

    CheckOperation(String name, Function<MicroProfileHealthReporter, SmallRyeHealth> operator) {
        this.definition = new SimpleOperationDefinitionBuilder(name, MicroProfileHealthExtension.SUBSYSTEM_RESOLVER)
                .setRuntimeOnly()
                .setReplyType(ModelType.OBJECT)
                .setReplyValueType(ModelType.OBJECT)
                .build();
        this.operator = operator;
    }

    @Override
    public OperationDefinition getOperationDefinition() {
        return this.definition;
    }

    @Override
    public ModelNode execute(ExpressionResolver resolver, ModelNode operation, MicroProfileHealthReporter reporter) throws OperationFailedException {
        SmallRyeHealth health = this.operator.apply(reporter);
        return ModelNode.fromJSONString(health.getPayload().toString());
    }
}
