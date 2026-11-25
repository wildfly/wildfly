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
    public static final String AMQP_CONNECTOR_ATTRIBUTE = "amqp-connector";
    public static final String KAFKA_CONNECTOR_ATTRIBUTE = "kafka-connector";

    public static void setModelValues(TracingType amqpTracingType, TracingType kafkaTracingType) {
        TracingTypeInterceptorFactory.AMQP_TRACING_TYPE = amqpTracingType;
        TracingTypeInterceptorFactory.KAFKA_TRACING_TYPE = kafkaTracingType;
    }
}
