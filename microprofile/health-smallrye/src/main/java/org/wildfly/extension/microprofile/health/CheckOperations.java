/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

    private static final OperationDefinition CHECK_DEFINITION = new SimpleOperationDefinitionBuilder("check", MicroProfileHealthExtension.getResourceDescriptionResolver(MicroProfileHealthExtension.SUBSYSTEM_NAME))
            .setRuntimeOnly()
            .setReplyType(ModelType.OBJECT)
            .setReplyValueType(ModelType.OBJECT)
            .setRuntimeOnly()
            .build();
    private static final OperationDefinition CHECK_LIVE_DEFINITION = new SimpleOperationDefinitionBuilder("check-live", MicroProfileHealthExtension.getResourceDescriptionResolver(MicroProfileHealthExtension.SUBSYSTEM_NAME))
            .setRuntimeOnly()
            .setReplyType(ModelType.OBJECT)
            .setReplyValueType(ModelType.OBJECT)
            .setRuntimeOnly()
            .build();
    private static final OperationDefinition CHECK_READY_DEFINITION = new SimpleOperationDefinitionBuilder("check-ready", MicroProfileHealthExtension.getResourceDescriptionResolver(MicroProfileHealthExtension.SUBSYSTEM_NAME))
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
