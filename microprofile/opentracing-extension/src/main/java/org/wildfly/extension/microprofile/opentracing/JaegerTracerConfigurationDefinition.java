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
package org.wildfly.extension.microprofile.opentracing;

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
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SENDER_BINDING;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SENDER_ENDPOINT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.TRACEID_128BIT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.TRACER_TAGS;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;


/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class JaegerTracerConfigurationDefinition extends SimpleResourceDefinition {

    public static final PathElement TRACER_CONFIGURATION_PATH = PathElement.pathElement("jaeger-tracer");

    public static final AttributeDefinition[] ATTRIBUTES = {PROPAGATION, SAMPLER_TYPE, SAMPLER_PARAM,
        SAMPLER_MANAGER_HOST_PORT, SENDER_BINDING, SENDER_ENDPOINT, SENDER_AUTH_TOKEN,
        SENDER_AUTH_USER, SENDER_AUTH_PASSWORD, REPORTER_LOG_SPANS, REPORTER_FLUSH_INTERVAL, REPORTER_MAX_QUEUE_SIZE,
        TRACER_TAGS, TRACEID_128BIT};
    private static final OperationStepHandler WRITE_HANDLER = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);

    public JaegerTracerConfigurationDefinition() {
        super(new SimpleResourceDefinition.Parameters(TRACER_CONFIGURATION_PATH, SubsystemExtension.getResourceDescriptionResolver("tracer"))
                .setAddHandler(JaegerTracerConfigurationAddHandler.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(TRACER_CAPABILITY)
                .setAdditionalPackages(RuntimePackageDependency.required("io.jaegertracing.jaeger")));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for(AttributeDefinition att : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(att, null, WRITE_HANDLER);
        }
    }

}
