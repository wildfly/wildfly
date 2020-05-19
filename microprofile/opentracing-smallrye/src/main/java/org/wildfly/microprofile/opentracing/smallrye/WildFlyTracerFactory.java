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
package org.wildfly.microprofile.opentracing.smallrye;

import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jboss.dmr.ModelNode;

/**
 * Registry of all the tracers configuration registered in WildFly.
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class WildFlyTracerFactory {

    private static final Map<String, Configuration> CONFIGURATIONS = Collections.synchronizedMap(new HashMap<>());

    public static final String TRACER_CAPABILITY_NAME = "org.wildfly.microprofile.opentracing.tracer";

    public static final String ENV_TRACER = "org.wildfly.microprofile.opentracing.env-tracer";

    private static final Configuration DEFAULT_CONFIGURATION = new Configuration();

    public static Consumer<TracerConfiguration> registerTracer(String service) {
        assert service != null;
        return CONFIGURATIONS.computeIfAbsent(service, name -> new Configuration());
    }

    public static Consumer<TracerConfiguration> registerDefaultTracer() {
        return DEFAULT_CONFIGURATION;
    }

    public static String getDefaultTracerName() {
        TracerConfiguration configuration = DEFAULT_CONFIGURATION.get();
        if (configuration != null) {
            return configuration.getName();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Tracer getTracer(String config, String serviceName) {
        if (serviceName != null && !serviceName.isEmpty()) {
            if (config != null && !config.isEmpty()) {
                TracerConfiguration configuration = CONFIGURATIONS.get(config).get();
                if (configuration != null) {
                    return configuration.createTracer(serviceName);
                }
            } else {
                TracerConfiguration configuration = CONFIGURATIONS.get(ENV_TRACER).get();
                if (configuration != null) {
                    return configuration.createTracer(serviceName);
                }
            }
        }
        return NoopTracerFactory.create();
    }

    @SuppressWarnings("unchecked")
    public static ModelNode getModel(String config, String serviceName) {
        if (serviceName != null && !serviceName.isEmpty()) {
            if (config != null) {
                TracerConfiguration configuration = CONFIGURATIONS.get(config).get();
                if (configuration != null) {
                    return configuration.getModel();
                }
            } else {
                TracerConfiguration configuration = CONFIGURATIONS.get(ENV_TRACER).get();
                if (configuration != null) {
                    return configuration.getModel();
                }
            }
        }
        return new ModelNode();
    }

    @SuppressWarnings("unchecked")
    public static Collection<String> getModules() {
        synchronized(CONFIGURATIONS) {
            return CONFIGURATIONS.values().stream().map(Configuration::get).filter(config -> config != null).map(TracerConfiguration::getModuleName).collect(Collectors.toSet());
        }
    }

    private static final class Configuration implements Consumer<TracerConfiguration> {

        private TracerConfiguration t;

        @Override
        public void accept(TracerConfiguration t) {
            this.t = t;
        }

        public TracerConfiguration get() {
            return t;
        }
    }
}
