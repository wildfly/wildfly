/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config;

/**
 * Utility class that translates between model values, and properties in the ConfigSource implementations
 * in this package
 */
public class ReactiveMessagingConfigSetter {
    public static void setModelValues(TracingType amqpTracingType, TracingType kafkaTracingType) {
        TracingTypeInterceptorFactory.AMQP_TRACING_TYPE = amqpTracingType;
        TracingTypeInterceptorFactory.KAFKA_TRACING_TYPE = kafkaTracingType;
    }
}
