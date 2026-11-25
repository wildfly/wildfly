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
    // Connector Tracing - checking mp.messaging.connector.smallrye-xxx.tracing-enabled

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

    private void tracingConnector(TracingType tracingType, Boolean connectorTracing, Boolean expected) {
        setInterceptorFactoryTracingType(tracingType);

        Map<String, String> map;
        if (connectorTracing != null) {
            map = Collections.singletonMap(this.connectorTracing, connectorTracing.toString());
        } else {
            map = Collections.emptyMap();
        }
        SmallRyeConfig config = createConfig(map);
        checkConfigValue(config, this.connectorTracing, expected);

    }

    ////////////////////////////////////////////////////////
    // Incoming Channel Tracing - checking mp.messaging.incoming.my-channel.tracing-enabled


    @Test
    public void testTracingNeverIncomingChannelNull() {
        tracingIncomingChannel(TracingType.NEVER, null, Boolean.FALSE);
    }

    @Test
    public void testTracingNeverIncomingChannelTrue() {
        tracingIncomingChannel(TracingType.NEVER, Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void testTracingNeverIncomingChannelFalse() {
        tracingIncomingChannel(TracingType.NEVER, Boolean.FALSE, Boolean.FALSE);
    }

    @Test
    public void testTracingAlwaysIncomingChannelNull() {
        tracingIncomingChannel(TracingType.ALWAYS, null, Boolean.TRUE);
    }

    @Test
    public void testTracingAlwaysIncomingChannelTrue() {
        tracingIncomingChannel(TracingType.ALWAYS, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void testTracingAlwaysIncomingChannelFalse() {
        tracingIncomingChannel(TracingType.ALWAYS, Boolean.FALSE, Boolean.TRUE);
    }

    @Test
    public void testTracingOffIncomingChannelNull() {
        tracingIncomingChannel(TracingType.OFF, null, null);
    }

    @Test
    public void testTracingOffIncomingChannelTrue() {
        tracingIncomingChannel(TracingType.OFF, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void testTracingOffIncomingChannelFalse() {
        tracingIncomingChannel(TracingType.OFF, Boolean.FALSE, Boolean.FALSE);
    }

    @Test
    public void testTracingOnIncomingChannelNull() {
        tracingIncomingChannel(TracingType.ON, null, null);
    }

    @Test
    public void testTracingOnIncomingChannelTrue() {
        tracingIncomingChannel(TracingType.ON, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void testTracingOnIncomingChannelFalse() {
        tracingIncomingChannel(TracingType.ON, Boolean.FALSE, Boolean.FALSE);
    }

    private void tracingIncomingChannel(TracingType tracingType, Boolean channelTracing, Boolean expected) {
        setInterceptorFactoryTracingType(tracingType);

        Map<String, String> map;
        if (channelTracing != null) {
            map = Map.of(
                    CHANNEL_INCOMING_CONNECTOR, connectorName,
                    CHANNEL_INCOMING_TRACING, channelTracing.toString());
        } else {
            map = Map.of(
                    CHANNEL_INCOMING_CONNECTOR, connectorName);
        }
        SmallRyeConfig config = createConfig(map);
        checkConfigValue(config, CHANNEL_INCOMING_TRACING, expected);
    }

    ////////////////////////////////////////////////////////
    // Outgoing Channel Tracing - checking mp.messaging.outgoing.my-channel.tracing-enabled

    @Test
    public void testTracingNeverOutgoingChannelNull() {
        tracingOutgoingChannel(TracingType.NEVER, null, Boolean.FALSE);
    }

    @Test
    public void testTracingNeverOutgoingChannelTrue() {
        tracingOutgoingChannel(TracingType.NEVER, Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void testTracingNeverOutgoingChannelFalse() {
        tracingOutgoingChannel(TracingType.NEVER, Boolean.FALSE, Boolean.FALSE);
    }

    @Test
    public void testTracingAlwaysOutgoingChannelNull() {
        tracingOutgoingChannel(TracingType.ALWAYS, null, Boolean.TRUE);
    }

    @Test
    public void testTracingAlwaysOutgoingChannelTrue() {
        tracingOutgoingChannel(TracingType.ALWAYS, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void testTracingAlwaysOutgoingChannelFalse() {
        tracingOutgoingChannel(TracingType.ALWAYS, Boolean.FALSE, Boolean.TRUE);
    }

    @Test
    public void testTracingOffOutgoingChannelNull() {
        tracingOutgoingChannel(TracingType.OFF, null, null);
    }

    @Test
    public void testTracingOffOutgoingChannelTrue() {
        tracingOutgoingChannel(TracingType.OFF, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void testTracingOffOutgoingChannelFalse() {
        tracingOutgoingChannel(TracingType.OFF, Boolean.FALSE, Boolean.FALSE);
    }

    @Test
    public void testTracingOnOutgoingChannelNull() {
        tracingOutgoingChannel(TracingType.ON, null, null);
    }

    @Test
    public void testTracingOnOutgoingChannelTrue() {
        tracingOutgoingChannel(TracingType.ON, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void testTracingOnOutgoingChannelFalse() {
        tracingOutgoingChannel(TracingType.ON, Boolean.FALSE, Boolean.FALSE);
    }

    private void tracingOutgoingChannel(TracingType tracingType, Boolean channelTracing, Boolean expected) {
        setInterceptorFactoryTracingType(tracingType);

        Map<String, String> map;
        if (channelTracing != null) {
            map = Map.of(
                    CHANNEL_OUTGOING_CONNECTOR, connectorName,
                    CHANNEL_OUTGOING_TRACING, channelTracing.toString());
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

    protected abstract void setInterceptorFactoryTracingType(TracingType tracingType);

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
