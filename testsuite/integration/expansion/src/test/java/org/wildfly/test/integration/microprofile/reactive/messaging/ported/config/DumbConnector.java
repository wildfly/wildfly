/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.ported.config;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import java.util.Locale;

/**
 * Copied from Quarkus and adjusted
 */
@ApplicationScoped
@Connector("dummy")
public class DumbConnector implements IncomingConnectorFactory {
    @Override
    public PublisherBuilder<? extends Message<?>> getPublisherBuilder(Config config) {
        String values = config.getValue("values", String.class);
        return ReactiveStreams.of(values, values.toUpperCase(Locale.ENGLISH))
                .map(Message::of);
    }
}
