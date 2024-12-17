/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.health;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.management.Capabilities;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2020 Red Hat inc.
 */
public class HealthSubsystemDefinition extends PersistentResourceDefinition {

    static final String HTTP_EXTENSIBILITY_CAPABILITY = "org.wildfly.management.http.extensible";
    public static final String HEALTH_HTTP_SECURITY_CAPABILITY = "org.wildfly.extension.health.http-context.security-enabled";

    static final RuntimeCapability<Void> HEALTH_HTTP_CONTEXT_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.extension.health.http-context", HealthContextService.class)
            .addRequirements(HTTP_EXTENSIBILITY_CAPABILITY)
            .build();
    static final RuntimeCapability<Void> SERVER_HEALTH_PROBES_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.extension.health.server-probes", ServerProbesService.class)
            .addRequirements(ModelControllerClientFactory.SERVICE_DESCRIPTOR, Capabilities.MANAGEMENT_EXECUTOR)
            .build();


    static final AttributeDefinition SECURITY_ENABLED = SimpleAttributeDefinitionBuilder.create("security-enabled", ModelType.BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .build();

    static final AttributeDefinition[] ATTRIBUTES = { SECURITY_ENABLED };

    protected HealthSubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(HealthExtension.SUBSYSTEM_PATH,
                HealthExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(HealthSubsystemAdd.INSTANCE)
                .setRemoveHandler(new ServiceRemoveStepHandler(HealthSubsystemAdd.INSTANCE))
                .addCapabilities(HEALTH_HTTP_CONTEXT_CAPABILITY, SERVER_HEALTH_PROBES_CAPABILITY));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

}