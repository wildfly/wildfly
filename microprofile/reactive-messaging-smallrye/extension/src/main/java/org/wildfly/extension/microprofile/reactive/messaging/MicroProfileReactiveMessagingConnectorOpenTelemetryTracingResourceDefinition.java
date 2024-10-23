/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.reactive.messaging;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.microprofile.reactive.messaging.config.TracingType;

public class MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition extends PersistentResourceDefinition {
    public static final PathElement PATH = PathElement.pathElement("opentelemetry-tracing", "config");

    static final MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition INSTANCE = new MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition();

    static final AttributeDefinition AMQP = SimpleAttributeDefinitionBuilder.create("amqp-connector", ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(false)
            .setValidator(EnumValidator.create(TracingType.class))
            .setDefaultValue(new ModelNode(TracingType.NEVER.toString()))
            .setRestartAllServices()
            .build();

    static final AttributeDefinition KAFKA = SimpleAttributeDefinitionBuilder.create("kafka-connector", ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(false)
            .setValidator(EnumValidator.create(TracingType.class))
            .setDefaultValue(new ModelNode(TracingType.NEVER.toString()))
            .setRestartAllServices()
            .build();

    static final List<AttributeDefinition> ATTRIBUTES = Arrays.asList(AMQP, KAFKA);

    private MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition() {
        super(
                new SimpleResourceDefinition.Parameters(
                        PATH,
                        MicroProfileReactiveMessagingExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH.getKey())
                )
                .setAddHandler(ReloadRequiredAddStepHandler.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)

                // TODO depend on OTel
                //.setCapabilities(REACTIVE_STREAMS_OPERATORS_CAPABILITY)

        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }
}
