/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.opentracing;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.PROPAGATION;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.REPORTER_FLUSH_INTERVAL;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.REPORTER_LOG_SPANS;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.REPORTER_MAX_QUEUE_SIZE;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SAMPLER_MANAGER_HOST_PORT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SAMPLER_PARAM;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SAMPLER_TYPE;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SENDER_BINDING;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SENDER_AUTH_PASSWORD;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SENDER_AUTH_TOKEN;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SENDER_AUTH_USER;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SENDER_ENDPOINT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.TRACEID_128BIT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.TRACER_TAGS;

import org.jboss.as.controller.PersistentResourceXMLDescription.PersistentResourceXMLBuilder;

public class SubsytemParser_3_0 extends PersistentResourceXMLParser {

    public static final String OPENTRACING_NAMESPACE = "urn:wildfly:microprofile-opentracing-smallrye:3.0";

    static final PersistentResourceXMLParser INSTANCE = new SubsytemParser_3_0();

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        PersistentResourceXMLBuilder jaegerTracer = builder(JaegerTracerConfigurationDefinition.TRACER_CONFIGURATION_PATH)
                .addAttributes(
                        PROPAGATION, SAMPLER_TYPE, SAMPLER_PARAM, SAMPLER_MANAGER_HOST_PORT,
                        SENDER_BINDING, SENDER_ENDPOINT, SENDER_AUTH_TOKEN,
                        SENDER_AUTH_USER, SENDER_AUTH_PASSWORD, REPORTER_LOG_SPANS,
                        REPORTER_FLUSH_INTERVAL, REPORTER_MAX_QUEUE_SIZE, TRACER_TAGS, TRACEID_128BIT
                );

        return builder(SubsystemExtension.SUBSYSTEM_PATH, OPENTRACING_NAMESPACE)
                .addAttributes(SubsystemDefinition.DEFAULT_TRACER)
                .addChild(jaegerTracer)
                .build();
    }
}
