/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.health;

import java.util.function.Function;

import io.smallrye.health.SmallRyeHealth;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

/**
 * Management operation that returns the DMR representation of the MicroProfile Health Check JSON payload.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class CheckOperations extends AbstractRuntimeOnlyHandler {

    private static final OperationDefinition CHECK_DEFINITION = new SimpleOperationDefinitionBuilder("check", MicroProfileHealthExtension.SUBSYSTEM_RESOLVER)
            .setRuntimeOnly()
            .setReplyType(ModelType.OBJECT)
            .setReplyValueType(ModelType.OBJECT)
            .setRuntimeOnly()
            .build();
    private static final OperationDefinition CHECK_LIVE_DEFINITION = new SimpleOperationDefinitionBuilder("check-live", MicroProfileHealthExtension.SUBSYSTEM_RESOLVER)
            .setRuntimeOnly()
            .setReplyType(ModelType.OBJECT)
            .setReplyValueType(ModelType.OBJECT)
            .setRuntimeOnly()
            .build();
    private static final OperationDefinition CHECK_READY_DEFINITION = new SimpleOperationDefinitionBuilder("check-ready", MicroProfileHealthExtension.SUBSYSTEM_RESOLVER)
            .setRuntimeOnly()
            .setReplyType(ModelType.OBJECT)
            .setReplyValueType(ModelType.OBJECT)
            .setRuntimeOnly()
            .build();
    private static final OperationDefinition CHECK_STARTED_DEFINITION = new SimpleOperationDefinitionBuilder("check-started", MicroProfileHealthExtension.SUBSYSTEM_RESOLVER)
        .setRuntimeOnly()
        .setReplyType(ModelType.OBJECT)
        .setReplyValueType(ModelType.OBJECT)
        .setRuntimeOnly()
        .build();


    private final Function<MicroProfileHealthReporter, SmallRyeHealth> healthOperation;

    public CheckOperations(Function<MicroProfileHealthReporter, SmallRyeHealth> healthOperation) {
        this.healthOperation = healthOperation;
    }

    static void register(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerOperationHandler(CHECK_DEFINITION, new CheckOperations((MicroProfileHealthReporter h) -> h.getHealth()));
        resourceRegistration.registerOperationHandler(CHECK_LIVE_DEFINITION, new CheckOperations((MicroProfileHealthReporter h) -> h.getLiveness()));
        resourceRegistration.registerOperationHandler(CHECK_READY_DEFINITION, new CheckOperations((MicroProfileHealthReporter h) -> h.getReadiness()));
        resourceRegistration.registerOperationHandler(CHECK_STARTED_DEFINITION, new CheckOperations((MicroProfileHealthReporter h) -> h.getStartup()));
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) {
        ServiceName serviceName = context.getCapabilityServiceName(MicroProfileHealthSubsystemDefinition.MICROPROFILE_HEALTH_REPORTER_CAPABILITY, MicroProfileHealthReporter.class);
        MicroProfileHealthReporter reporter = (MicroProfileHealthReporter) context.getServiceRegistry(false).getService(serviceName).getValue();

        SmallRyeHealth health = healthOperation.apply(reporter);
        ModelNode result = ModelNode.fromJSONString(health.getPayload().toString());
        context.getResult().set(result);
    }
}
