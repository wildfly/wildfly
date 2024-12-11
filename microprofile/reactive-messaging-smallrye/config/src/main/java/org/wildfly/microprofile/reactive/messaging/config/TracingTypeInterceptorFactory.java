/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config;

import jakarta.annotation.Priority;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.Priorities;

@Priority(Priorities.APPLICATION + 1900)
public class TracingTypeInterceptorFactory implements ConfigSourceInterceptorFactory {
    static TracingType AMQP_TRACING_TYPE;
    static TracingType KAFKA_TRACING_TYPE;

    @Override
    public ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext context) {
        return new TracingTypeInterceptor(AMQP_TRACING_TYPE, KAFKA_TRACING_TYPE);
    }

}
