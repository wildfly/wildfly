/*
 * Copyright 2019 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.microprofile.opentracing.resolver;

import static org.wildfly.extension.microprofile.opentracing.JaegerTracerConfigurationDefinition.ATTRIBUTES;
import static org.wildfly.extension.microprofile.opentracing.SubsystemDefinition.TRACER_CAPABILITY;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.PROPAGATION;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.REPORTER_FLUSH_INTERVAL;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.REPORTER_LOG_SPANS;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.REPORTER_MAX_QUEUE_SIZE;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SAMPLER_MANAGER_HOST_PORT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SAMPLER_PARAM;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SAMPLER_TYPE;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SENDER_AUTH_PASSWORD;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SENDER_AUTH_TOKEN;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SENDER_AUTH_USER;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SENDER_ENDPOINT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.TRACEID_128BIT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.TRACER_TAGS;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.CodecConfiguration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.opentracing.Tracer;
import java.util.Map;
import java.util.function.Supplier;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.wildfly.microprofile.opentracing.smallrye.TracerConfiguration;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class JaegerTracerConfiguration implements TracerConfiguration {

    private final CodecConfiguration codecConfig;
    private final SamplerConfiguration samplerConfig;
    private final ReporterConfiguration reporterConfig;
    private final Supplier<OutboundSocketBinding> outboundSocketBindingSupplier;
    private final boolean traceId128Bit;
    private final Map<String, String> tracerTags;
    private final ModelNode model;
    private final String name;

    public JaegerTracerConfiguration(ExpressionResolver context, String name, ModelNode configuration, Supplier<OutboundSocketBinding> outboundSocketBindingSupplier) throws OperationFailedException {
        this.name= TRACER_CAPABILITY.getCapabilityServiceName(name).getCanonicalName();
        this.outboundSocketBindingSupplier = outboundSocketBindingSupplier;
        model = new ModelNode();
        for(AttributeDefinition att : ATTRIBUTES) {
            ModelNode value = att.resolveModelAttribute(context, configuration);
            if(value.isDefined()) {
                model.get(att.getName()).set(value);
            }
        }
        model.protect();
        codecConfig = new CodecConfiguration();
        for (String codec : PROPAGATION.unwrap(context, configuration)) {
            codecConfig.withPropagation(Configuration.Propagation.valueOf(codec));
        }
        samplerConfig = new SamplerConfiguration()
                .withType(SAMPLER_TYPE.resolveModelAttribute(context, configuration).asStringOrNull())
                .withParam(SAMPLER_PARAM.resolveModelAttribute(context, configuration).asDoubleOrNull())
                .withManagerHostPort(SAMPLER_MANAGER_HOST_PORT.resolveModelAttribute(context, configuration).asStringOrNull());
        Configuration.SenderConfiguration senderConfiguration = new Configuration.SenderConfiguration()
                .withAuthPassword(SENDER_AUTH_PASSWORD.resolveModelAttribute(context, configuration).asStringOrNull())
                .withAuthUsername(SENDER_AUTH_USER.resolveModelAttribute(context, configuration).asStringOrNull())
                .withAuthToken(SENDER_AUTH_TOKEN.resolveModelAttribute(context, configuration).asStringOrNull())
                .withEndpoint(SENDER_ENDPOINT.resolveModelAttribute(context, configuration).asStringOrNull());
        reporterConfig = new ReporterConfiguration()
                .withSender(senderConfiguration)
                .withFlushInterval(REPORTER_FLUSH_INTERVAL.resolveModelAttribute(context, configuration).asIntOrNull())
                .withLogSpans(REPORTER_LOG_SPANS.resolveModelAttribute(context, configuration).asBooleanOrNull())
                .withMaxQueueSize(REPORTER_MAX_QUEUE_SIZE.resolveModelAttribute(context, configuration).asIntOrNull());
        tracerTags = TRACER_TAGS.unwrap(context, configuration);
        traceId128Bit = TRACEID_128BIT.resolveModelAttribute(context, configuration).asBoolean();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Tracer createTracer(String serviceName) {
        return createConfiguration(serviceName)
                .getTracerBuilder()
                    .withManualShutdown()
                    .build();
    }

    /**
     * For testing purpose only.
     * @param serviceName
     * @return the jaeger tracer configuration.
     */
    Configuration createConfiguration(String serviceName) {
        Configuration.SenderConfiguration senderConfiguration = reporterConfig.getSenderConfiguration();
        OutboundSocketBinding outboundSocketBinding = outboundSocketBindingSupplier.get();
        if(outboundSocketBinding != null) {
            senderConfiguration.withAgentHost(outboundSocketBinding.getUnresolvedDestinationAddress())
                    .withAgentPort(outboundSocketBinding.getDestinationPort());
        }
        return new Configuration(serviceName)
                .withCodec(codecConfig)
                .withReporter(reporterConfig)
                .withSampler(samplerConfig)
                .withTraceId128Bit(traceId128Bit)
                .withTracerTags(tracerTags);
    }
    @Override
    public String getModuleName() {
        return "io.jaegertracing.jaeger";
    }

    @Override
    public ModelNode getModel() {
        return model;
    }
}
