/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.opentracing;


import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.StringAllowedValuesValidator;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Tracer attribute definitions.
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class TracerAttributes {

    private static final String[] ALLOWED_SAMPLER_TYPE = {"const", "probabilistic", "ratelimiting", "remote"};

    public static final StringListAttributeDefinition PROPAGATION = StringListAttributeDefinition.Builder.of(TracerConfigurationConstants.PROPAGATION)
            .setAllowNullElement(false)
            .setRequired(false)
            .setValidator(new StringAllowedValuesValidator("JAEGER", "B3"))
            .setAttributeGroup("codec-configuration")
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition SAMPLER_TYPE = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SAMPLER_TYPE, ModelType.STRING, true)
            .setValidator(new StringAllowedValuesValidator(ALLOWED_SAMPLER_TYPE))
            .setDefaultValue(new ModelNode("remote"))
            .setAttributeGroup("sampler-configuration")
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SAMPLER_PARAM = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SAMPLER_PARAM, ModelType.DOUBLE, true)
            .setAttributeGroup("sampler-configuration")
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SAMPLER_MANAGER_HOST_PORT = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SAMPLER_MANAGER_HOST_PORT, ModelType.STRING, true)
            .setAttributeGroup("sampler-configuration")
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition SENDER_BINDING = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SENDER_AGENT_BINDING, ModelType.STRING, true)
            .setAttributeGroup("sender-configuration")
            .setCapabilityReference(OutboundSocketBinding.SERVICE_DESCRIPTOR.getName())
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SENDER_ENDPOINT = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SENDER_ENDPOINT, ModelType.STRING, true)
            .setAttributeGroup("sender-configuration")
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SENDER_AUTH_TOKEN = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SENDER_AUTH_TOKEN, ModelType.STRING, true)
            .setAttributeGroup("sender-configuration")
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SENDER_AUTH_USER = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SENDER_AUTH_USER, ModelType.STRING, true)
            .setAttributeGroup("sender-configuration")
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SENDER_AUTH_PASSWORD = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SENDER_AUTH_PASSWORD, ModelType.STRING, true)
            .setAttributeGroup("sender-configuration")
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition REPORTER_LOG_SPANS = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.REPORTER_LOG_SPANS, ModelType.BOOLEAN, true)
            .setAttributeGroup("reporter-configuration")
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition REPORTER_FLUSH_INTERVAL = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.REPORTER_FLUSH_INTERVAL, ModelType.INT, true)
            .setAttributeGroup("reporter-configuration")
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition REPORTER_MAX_QUEUE_SIZE = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.REPORTER_MAX_QUEUE_SIZE, ModelType.INT, true)
            .setAttributeGroup("reporter-configuration")
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition TRACEID_128BIT = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.TRACEID_128BIT, ModelType.BOOLEAN, true)
            .setDefaultValue(ModelNode.FALSE)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final PropertiesAttributeDefinition TRACER_TAGS = new PropertiesAttributeDefinition.Builder(TracerConfigurationConstants.TRACER_TAGS, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
}
