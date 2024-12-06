/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config;

import java.util.Collections;
import java.util.Map;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public abstract class TracingTypeConfigTest {

    private static final String CONNECTOR_TRACING_TEMPLATE = "mp.messaging.connector.%s.tracing-enabled";

    private static final String CHANNEL_OUTGOING_CONNECTOR = "mp.messaging.outgoing.my-channel.connector";
    private static final String CHANNEL_INCOMING_CONNECTOR = "mp.messaging.incoming.my-channel.connector";

    private static final String CHANNEL_OUTGOING_TRACING = "mp.messaging.outgoing.my-channel.tracing-enabled";
    private static final String CHANNEL_INCOMING_TRACING = "mp.messaging.incoming.my-channel.tracing-enabled";


    private final String connectorName;
    private final String connectorTracing;

    public TracingTypeConfigTest(String connectorName) {
        this.connectorName = connectorName;
        this.connectorTracing = String.format(CONNECTOR_TRACING_TEMPLATE, connectorName);
    }

    @Before
    public void before() {
        TracingTypeInterceptorFactory.AMQP_TRACING_TYPE = TracingType.NEVER;
        TracingTypeInterceptorFactory.KAFKA_TRACING_TYPE = TracingType.NEVER;
    }

    ////////////////////////////////////////////////////////
    // AMQP Connector Tracing - checking mp.messaging.connector.smallrye-amqp.tracing-enabled

    @Test
    public void testTracingNeverConnectorNull() {
        tracingConnector(TracingType.NEVER, null, Boolean.FALSE);
    }

    @Test
    public void testTracingNeverConnectorTrue() {
        tracingConnector(TracingType.NEVER, Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void testTracingNeverConnectorFalse() {
        tracingConnector(TracingType.NEVER, Boolean.FALSE, Boolean.FALSE);
    }

    @Test
    public void testTracingAlwaysConnectorNull() {
        tracingConnector(TracingType.ALWAYS, null, Boolean.TRUE);
    }

    @Test
    public void testTracingAlwaysConnectorTrue() {
        tracingConnector(TracingType.ALWAYS, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void testTracingAlwaysConnectorFalse() {
        tracingConnector(TracingType.ALWAYS, Boolean.FALSE, Boolean.TRUE);
    }

    @Test
    public void testTracingOffConnectorNull() {
        tracingConnector(TracingType.OFF, null, Boolean.FALSE);
    }

    @Test
    public void testTracingOffConnectorTrue() {
        tracingConnector(TracingType.OFF, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void testTracingOffConnectorFalse() {
        tracingConnector(TracingType.OFF, Boolean.FALSE, Boolean.FALSE);
    }

    @Test
    public void testTracingOnConnectorNull() {
        tracingConnector(TracingType.ON, null, Boolean.TRUE);
    }

    @Test
    public void testTracingOnConnectorTrue() {
        tracingConnector(TracingType.ON, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void testTracingOnConnectorFalse() {
        tracingConnector(TracingType.ON, Boolean.FALSE, Boolean.FALSE);
    }

    private void tracingConnector(TracingType tracingType, Boolean connectorAmqpTracing, Boolean expected) {
        TracingTypeInterceptorFactory.AMQP_TRACING_TYPE = tracingType;

        Map<String, String> map;
        if (connectorAmqpTracing != null) {
            map = Collections.singletonMap(connectorTracing, connectorAmqpTracing.toString());
        } else {
            map = Collections.emptyMap();
        }
        SmallRyeConfig config = createConfig(map);
        checkConfigValue(config, connectorTracing, expected);

    }

    ////////////////////////////////////////////////////////
    // AMQP Incoming Channel Tracing - checking mp.messaging.incoming.my-channel.tracing-enabled


    @Test
    public void testTracingNeverIncomingChannelNull() {
        amqpTracingIncomingChannel(TracingType.NEVER, null, Boolean.FALSE);
    }

    @Test
    public void testTracingNeverIncomingChannelTrue() {
        amqpTracingIncomingChannel(TracingType.NEVER, Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void testTracingNeverIncomingChannelFalse() {
        amqpTracingIncomingChannel(TracingType.NEVER, Boolean.FALSE, Boolean.FALSE);
    }

    @Test
    public void testTracingAlwaysIncomingChannelNull() {
        amqpTracingIncomingChannel(TracingType.ALWAYS, null, Boolean.TRUE);
    }

    @Test
    public void testTracingAlwaysIncomingChannelTrue() {
        amqpTracingIncomingChannel(TracingType.ALWAYS, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void testTracingAlwaysIncomingChannelFalse() {
        amqpTracingIncomingChannel(TracingType.ALWAYS, Boolean.FALSE, Boolean.TRUE);
    }

    @Test
    public void testTracingOffIncomingChannelNull() {
        amqpTracingIncomingChannel(TracingType.OFF, null, null);
    }

    @Test
    public void testTracingOffIncomingChannelTrue() {
        amqpTracingIncomingChannel(TracingType.OFF, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void testTracingOffIncomingChannelFalse() {
        amqpTracingIncomingChannel(TracingType.OFF, Boolean.FALSE, Boolean.FALSE);
    }

    @Test
    public void testTracingOnIncomingChannelNull() {
        amqpTracingIncomingChannel(TracingType.ON, null, null);
    }

    @Test
    public void testTracingOnIncomingChannelTrue() {
        amqpTracingIncomingChannel(TracingType.ON, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void testTracingOnIncomingChannelFalse() {
        amqpTracingIncomingChannel(TracingType.ON, Boolean.FALSE, Boolean.FALSE);
    }

    private void amqpTracingIncomingChannel(TracingType tracingType, Boolean channelAmqpTracing, Boolean expected) {
        TracingTypeInterceptorFactory.AMQP_TRACING_TYPE = tracingType;

        Map<String, String> map;
        if (channelAmqpTracing != null) {
            map = Map.of(
                    CHANNEL_INCOMING_CONNECTOR, connectorName,
                    CHANNEL_INCOMING_TRACING, channelAmqpTracing.toString());
        } else {
            map = Map.of(
                    CHANNEL_INCOMING_CONNECTOR, connectorName);
        }
        SmallRyeConfig config = createConfig(map);
        checkConfigValue(config, CHANNEL_INCOMING_TRACING, expected);
    }

    ////////////////////////////////////////////////////////
    // AMQP Outgoing Channel Tracing - checking mp.messaging.outgoing.my-channel.tracing-enabled

    @Test
    public void testTracingNeverOutgoingChannelNull() {
        amqpTracingOutgoingChannel(TracingType.NEVER, null, Boolean.FALSE);
    }

    @Test
    public void testTracingNeverOutgoingChannelTrue() {
        amqpTracingOutgoingChannel(TracingType.NEVER, Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void testTracingNeverOutgoingChannelFalse() {
        amqpTracingOutgoingChannel(TracingType.NEVER, Boolean.FALSE, Boolean.FALSE);
    }

    @Test
    public void testTracingAlwaysOutgoingChannelNull() {
        amqpTracingOutgoingChannel(TracingType.ALWAYS, null, Boolean.TRUE);
    }

    @Test
    public void testTracingAlwaysOutgoingChannelTrue() {
        amqpTracingOutgoingChannel(TracingType.ALWAYS, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void testTracingAlwaysOutgoingChannelFalse() {
        amqpTracingOutgoingChannel(TracingType.ALWAYS, Boolean.FALSE, Boolean.TRUE);
    }

    @Test
    public void testTracingOffOutgoingChannelNull() {
        amqpTracingOutgoingChannel(TracingType.OFF, null, null);
    }

    @Test
    public void testTracingOffOutgoingChannelTrue() {
        amqpTracingOutgoingChannel(TracingType.OFF, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void testTracingOffOutgoingChannelFalse() {
        amqpTracingOutgoingChannel(TracingType.OFF, Boolean.FALSE, Boolean.FALSE);
    }

    @Test
    public void testTracingOnOutgoingChannelNull() {
        amqpTracingOutgoingChannel(TracingType.ON, null, null);
    }

    @Test
    public void testTracingOnOutgoingChannelTrue() {
        amqpTracingOutgoingChannel(TracingType.ON, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void testTracingOnOutgoingChannelFalse() {
        amqpTracingOutgoingChannel(TracingType.ON, Boolean.FALSE, Boolean.FALSE);
    }

    private void amqpTracingOutgoingChannel(TracingType tracingType, Boolean channelAmqpTracing, Boolean expected) {
        TracingTypeInterceptorFactory.AMQP_TRACING_TYPE = tracingType;

        Map<String, String> map;
        if (channelAmqpTracing != null) {
            map = Map.of(
                    CHANNEL_OUTGOING_CONNECTOR, connectorName,
                    CHANNEL_OUTGOING_TRACING, channelAmqpTracing.toString());
        } else {
            map = Map.of(
                    CHANNEL_OUTGOING_CONNECTOR, connectorName);
        }
        SmallRyeConfig config = createConfig(map);
        checkConfigValue(config, CHANNEL_OUTGOING_TRACING, expected);
    }

    private void checkConfigValue(SmallRyeConfig config, String property, Boolean expected) {
        if (expected != null) {
            Assert.assertEquals(expected.toString(), config.getConfigValue(property).getValue());
        } else {
            Assert.assertNull(config.getConfigValue(property).getValue());
        }

    }


    private SmallRyeConfig createConfig(Map<String, String> values) {
        return new SmallRyeConfigBuilder()
        .addDefaultSources()
        .addDefaultInterceptors()
        .addDiscoveredInterceptors()
        .withSources(new MapBackedConfigSource("Test", values) {
                })
                .build();
    }
}
