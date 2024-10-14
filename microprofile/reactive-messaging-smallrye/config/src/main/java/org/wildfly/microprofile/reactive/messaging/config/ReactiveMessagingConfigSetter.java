/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config;

import org.wildfly.microprofile.reactive.messaging.config._private.MicroProfileReactiveMessagingConfigLogger;

/**
 * Utility class that translates between model values, and properties in the ConfigSource implementations
 * in this package
 */
public class ReactiveMessagingConfigSetter {
    private static final String AMQP_TRACING_ENABLED_PROPERTY = "mp.messaging.connector.smallrye-amqp.tracing-enabled";

    private static final String KAFKA_TRACING_ENABLED_PROPERTY = "mp.messaging.connector.smallrye-kafka.tracing-enabled";


    public static void setModelValues(TracingType amqpTracingType, TracingType kafkaTracingType) {
        if (MicroProfileReactiveMessagingConfigLogger.LOGGER.isDebugEnabled()) {
            MicroProfileReactiveMessagingConfigLogger.LOGGER.debugf(
                    "Setting values in the ConfigProviders, amqp: %s, kafka: %s", amqpTracingType, kafkaTracingType);
        }
        setProperty(AMQP_TRACING_ENABLED_PROPERTY, amqpTracingType);
        setProperty(KAFKA_TRACING_ENABLED_PROPERTY, kafkaTracingType);
    }

    private static void setProperty(String property, TracingType tracingType) {
        if (tracingType == TracingType.NEVER) {
            ReactiveMessagingLockedValuesConfigSource.PROPERTIES.put(property, "false");
        } else if (tracingType == TracingType.OFF) {
            ReactiveMessagingDefaultValuesConfigSource.PROPERTIES.put(property, "false");
        } else if (tracingType == TracingType.ON) {
            ReactiveMessagingDefaultValuesConfigSource.PROPERTIES.put(property, "true");
        } else if (tracingType == TracingType.ALWAYS) {
            ReactiveMessagingLockedValuesConfigSource.PROPERTIES.put(property, "true");
        }
    }

}
