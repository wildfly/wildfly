/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

/**
 * ConfigSourceInterceptor to intercept Config reads of properties controlling whether tracing is enabled or not.
 *
 * The TracingTypes set in the subsystem configuration further control whether tracing is enabled or not, and if it is
 * default behaviour if these properties are not set.
 *
 * SmallRye's ConnectorConfig class will check these properties for each channel. First it will check for tracing
 * on the channel level, and if nothing is found it will check on connector level. *
 */
class TracingTypeInterceptor implements ConfigSourceInterceptor {

    private static final String SMALLRYE_AMQP = "smallrye-amqp";
    private static final String SMALLRYE_KAFKA = "smallrye-kafka";

    private static final String TRACING_ENABLED = ".tracing-enabled";
    private static final String CONNECTOR = ".connector";
    private static final String MP_MESSAGING_PREFIX = "mp.messaging.";

    private static final String CONNECTOR_PREFIX = MP_MESSAGING_PREFIX + "connector.";
    private static final String INCOMING_PREFIX = MP_MESSAGING_PREFIX + "incoming.";
    private static final String OUTGOING_PREFIX = MP_MESSAGING_PREFIX + "outgoing.";

    private final TracingType amqpTracingType;
    private final TracingType kafkaTracingType;



    TracingTypeInterceptor(TracingType amqpTracingType, TracingType kafkaTracingType) {
        this.amqpTracingType = amqpTracingType;
        this.kafkaTracingType = kafkaTracingType;
    }

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        if (name.startsWith(MP_MESSAGING_PREFIX) && name.endsWith(TRACING_ENABLED)) {
            TracingEnabledContext tracingContext = new TracingEnabledContext(context, name);
            return tracingContext.handleTracingProperty();
        }
        return context.proceed(name);
    }


    private class TracingEnabledContext {
        final ConfigSourceInterceptorContext interceptorContext;
        final String name;

        Connector connector;
        String channel;
        private TracingType tracingType;

        TracingEnabledContext(ConfigSourceInterceptorContext context, String name) {
            interceptorContext = context;
            this.name = name;
        }

        private ConfigValue handleTracingProperty() {
            determineConnectorAndTracingType();


            ConfigValue value = interceptorContext.proceed(name);
            if (tracingType == null) {
                return value;
            }

            switch (tracingType) {
                // NEVER and ALWAYS both disregard the tracing properties, and force tracing to either be
                // totally disabled or always enabled
                case NEVER:
                    return configValue(name, Boolean.FALSE);
                case ALWAYS:
                    return configValue(name, Boolean.TRUE);
                // For OFF/ON, we can override the default values with the values of the tracing properties.
                // Note that SmallRye's ConnectorConfig (which is the consumer of these properties), it will first
                // check the channel level property and use the value of that if it is set. If it is not set it
                // will check the connector level property.
                // Thus, in this interceptor, if checking the channel level property, and it is not set, we don't return
                // a default value (since that would make ConnectorConfig skip checking the connector level
                // property). If the connector level property is not set, we return the default.
                case OFF:
                    return defaultIfNotSetAndNotChannel(name, value, Boolean.FALSE);
                case ON:
                    return defaultIfNotSetAndNotChannel(name, value, Boolean.TRUE);
            }
            return null;
        }


        private ConfigValue defaultIfNotSetAndNotChannel(String name, ConfigValue foundValue, Boolean defaultValue) {
            if (foundValue != null && foundValue.getValue() != null) {
                return foundValue;
            }

            ConfigValue.ConfigValueBuilder builder = foundValue == null ?
                    new ConfigValue.ConfigValueBuilder().withName(name) :
                    foundValue.from();
            if (channel == null) {
                builder.withValue(defaultValue.toString());
            }

            return builder.build();
        }

        private ConfigValue configValue(String name, Boolean value) {
            return new ConfigValue.ConfigValueBuilder()
                    .withName(name)
                    .withValue(value == null ? null : value.toString())
                    .build();
        }

        private void determineConnectorAndTracingType() {
            if (name.startsWith(CONNECTOR_PREFIX)) {
                this.connector = Connector.fromName(stripPrefixAndTracingEnabledSuffix(name, CONNECTOR_PREFIX));
            } else {
                // For mp.mp.messaging.incoming and mp.messaging.outgoing,
                // we need to do an additional config read to get the name of the connector
                // We will have something like:
                //  mp.messaging.incoming.mychannel.tracing-enabled
                // And need to read
                //  mp.messaging.incoming.mychannel.connector
                // If mp.messaging.incoming.mychannel.connector has the value smallrye-amqp or smallrye-kafka
                // we infer the tracing type
                String prefix = null;
                if (name.startsWith(INCOMING_PREFIX)) {
                    prefix = INCOMING_PREFIX;
                    this.channel = stripPrefixAndTracingEnabledSuffix(name, INCOMING_PREFIX);
                } else if (name.startsWith(OUTGOING_PREFIX)) {
                    prefix = OUTGOING_PREFIX;
                    this.channel = stripPrefixAndTracingEnabledSuffix(name, OUTGOING_PREFIX);
                }

                if (prefix != null) {
                    String channelConnectorProperty = prefix + channel + CONNECTOR;
                    ConfigValue value = interceptorContext.restart(channelConnectorProperty);
                    if (value != null) {
                        String connector = value.getValue();
                        this.connector = Connector.fromName(connector);
                    }
                }
            }

            if (connector != null) {
                switch (connector) {
                    case AMQP:
                        this.tracingType = amqpTracingType;
                        break;
                    case KAFKA:
                        this.tracingType = kafkaTracingType;
                        break;
                }
            }
        }

        private String stripPrefixAndTracingEnabledSuffix(String name, String prefix) {
            String temp = name.substring(prefix.length());
            int index = temp.indexOf(TRACING_ENABLED);
            return temp.substring(0, index);
        }

    }


    private enum Connector {
        AMQP,
        KAFKA;

        static Connector fromName(String name) {
            if (name.equals(SMALLRYE_AMQP)) {
                return AMQP;
            } else if (name.equals(SMALLRYE_KAFKA)) {
                return KAFKA;
            }
            return null;
        }
    }
}
