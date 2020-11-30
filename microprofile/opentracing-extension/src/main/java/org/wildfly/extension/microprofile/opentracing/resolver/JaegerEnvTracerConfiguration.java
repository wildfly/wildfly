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

import static org.wildfly.microprofile.opentracing.smallrye.WildFlyTracerFactory.ENV_TRACER;

import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import java.util.Map;
import java.util.Properties;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.microprofile.opentracing.WildflyJaegerMetricsFactory;
import org.wildfly.microprofile.opentracing.smallrye.TracerConfiguration;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Jaeger tracer configuration relying on environment variables.
 * Added for compatibility.
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class JaegerEnvTracerConfiguration implements TracerConfiguration {
    private static final String JAEGER_ENV_PREFIX = "JAEGER_";

    @Override
    public Tracer createTracer(String serviceName) {
        return Configuration.fromEnv(serviceName)
                .getTracerBuilder()
                    .withMetricsFactory(new WildflyJaegerMetricsFactory())
                    .withManualShutdown()
                    .build();
    }

    @Override
    public String getModuleName() {
        return "io.jaegertracing.jaeger";
    }

    @Override
    public ModelNode getModel() {
        ModelNode model = new ModelNode();
        model.get("class").set("io.jaegertracing.internal.JaegerTracer");
        ModelNode configuration = new ModelNode();
        for(Map.Entry<String, String> envVariable : WildFlySecurityManager.getSystemEnvironmentPrivileged().entrySet()) {
            if(envVariable.getKey().startsWith(JAEGER_ENV_PREFIX)) {
                configuration.get(envVariable.getKey()).set(envVariable.getValue());
            }
        }
        Properties properties =  WildFlySecurityManager.getSystemPropertiesPrivileged();
        for(String property : properties.stringPropertyNames()) {
            if(property.startsWith(JAEGER_ENV_PREFIX)) {
                configuration.get(property).set(properties.getProperty(property));
            }
        }
        model.get("configuration").set(configuration);
        return model;
    }

    @Override
    public String getName() {
        return ENV_TRACER;
    }

}
