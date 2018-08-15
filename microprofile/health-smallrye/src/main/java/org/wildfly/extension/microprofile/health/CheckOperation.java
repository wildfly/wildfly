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

import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;
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
public class CheckOperation extends AbstractRuntimeOnlyHandler {

    private static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder("check", MicroProfileHealthExtension.getResourceDescriptionResolver(MicroProfileHealthExtension.SUBSYSTEM_NAME))
            .setRuntimeOnly()
            .setReplyType(ModelType.OBJECT)
            .setReplyValueType(ModelType.OBJECT)
            .build();

    static void register(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerOperationHandler(DEFINITION, new CheckOperation());
    }

    private CheckOperation() {
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) {
        ServiceName serviceName = context.getCapabilityServiceName(MicroProfileHealthSubsystemDefinition.HEALTH_REPORTER_CAPABILITY, SmallRyeHealthReporter.class);
        SmallRyeHealthReporter reporter = (SmallRyeHealthReporter) context.getServiceRegistry(false).getService(serviceName).getValue();

        SmallRyeHealth health = reporter.getHealth();
        ModelNode result = ModelNode.fromJSONString(health.getPayload().toString());
        context.getResult().set(result);
    }
}
