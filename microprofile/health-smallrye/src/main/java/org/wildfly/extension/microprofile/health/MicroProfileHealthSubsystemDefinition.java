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

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.health.HealthSubsystemDefinition;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class MicroProfileHealthSubsystemDefinition extends PersistentResourceDefinition {

    static final String MICROPROFILE_HEALTH_REPORTER_CAPABILITY = "org.wildfly.extension.microprofile.health.reporter";
    static final String HEALTH_HTTP_CONTEXT_CAPABILITY = "org.wildfly.extension.health.http-context";
    static final String HEALTH_SERVER_PROBE_CAPABILITY = "org.wildfly.extension.health.server-probes";

    static final RuntimeCapability<Void> HEALTH_REPORTER_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(MICROPROFILE_HEALTH_REPORTER_CAPABILITY, MicroProfileHealthReporter.class)
                    .addRequirements(WELD_CAPABILITY_NAME, HEALTH_SERVER_PROBE_CAPABILITY)
                    .build();

    public static final ServiceName HEALTH_REPORTER_SERVICE = ServiceName.parse(MICROPROFILE_HEALTH_REPORTER_CAPABILITY);

    static final RuntimeCapability<Void> MICROPROFILE_HEALTH_HTTP_CONTEXT_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.extension.microprofile.health.http-context", MicroProfileHealthContextService.class)
            .addRequirements(HEALTH_HTTP_CONTEXT_CAPABILITY)
            .build();
    static final ServiceName HTTP_CONTEXT_SERVICE = MICROPROFILE_HEALTH_HTTP_CONTEXT_CAPABILITY.getCapabilityServiceName();

    static final RuntimeCapability<Void> MICROPROFILE_HEALTH_HTTP_SECURITY_CAPABILITY = RuntimeCapability.Builder.of(HealthSubsystemDefinition.HEALTH_HTTP_SECURITY_CAPABILITY, Boolean.class)
            .build();

    static final AttributeDefinition SECURITY_ENABLED = SimpleAttributeDefinitionBuilder.create("security-enabled", ModelType.BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .build();

    static final AttributeDefinition EMPTY_LIVENESS_CHECKS_STATUS = SimpleAttributeDefinitionBuilder.create("empty-liveness-checks-status", ModelType.STRING)
            .setDefaultValue(new ModelNode("UP"))
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .setAllowedValues("UP", "DOWN")
            .build();

    static final AttributeDefinition EMPTY_READINESS_CHECKS_STATUS = SimpleAttributeDefinitionBuilder.create("empty-readiness-checks-status", ModelType.STRING)
            .setDefaultValue(new ModelNode("UP"))
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .setAllowedValues("UP", "DOWN")
            .build();

    static final AttributeDefinition EMPTY_STARTUP_CHECKS_STATUS = SimpleAttributeDefinitionBuilder.create("empty-startup-checks-status", ModelType.STRING)
        .setDefaultValue(new ModelNode("UP"))
        .setRequired(false)
        .setRestartAllServices()
        .setAllowExpression(true)
        .setAllowedValues("UP", "DOWN")
        .build();


    static final AttributeDefinition[] ATTRIBUTES = { SECURITY_ENABLED, EMPTY_LIVENESS_CHECKS_STATUS, EMPTY_READINESS_CHECKS_STATUS, EMPTY_STARTUP_CHECKS_STATUS};
    private boolean registerRuntimeOperations;

    protected MicroProfileHealthSubsystemDefinition(boolean registerRuntimeOperations) {
        super(new Parameters(MicroProfileHealthExtension.SUBSYSTEM_PATH,
                MicroProfileHealthExtension.getResourceDescriptionResolver(MicroProfileHealthExtension.SUBSYSTEM_NAME))
                .setAddHandler(MicroProfileHealthSubsystemAdd.INSTANCE)
                .setRemoveHandler(new ServiceRemoveStepHandler(MicroProfileHealthSubsystemAdd.INSTANCE))
                .setCapabilities(HEALTH_REPORTER_RUNTIME_CAPABILITY, MICROPROFILE_HEALTH_HTTP_CONTEXT_CAPABILITY, MICROPROFILE_HEALTH_HTTP_SECURITY_CAPABILITY));
        this.registerRuntimeOperations = registerRuntimeOperations;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        if (registerRuntimeOperations) {
            CheckOperations.register(resourceRegistration);
        }
    }


}
