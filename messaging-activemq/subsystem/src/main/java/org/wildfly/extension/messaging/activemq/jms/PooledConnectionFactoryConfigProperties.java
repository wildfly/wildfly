/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

/**
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 *         Date: 5/16/11
 *         Time: 5:09 PM
 */
public class PooledConnectionFactoryConfigProperties {
    private final String name;

    private final String value;

    private String type;
    private final ConnectionFactoryAttribute.ConfigType configType;

    /**
     * @param configType can be {@code null} to configure a property on the resource adapter.
     */
    public PooledConnectionFactoryConfigProperties(String name, String value, String type, ConnectionFactoryAttribute.ConfigType configType) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.configType = configType;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public ConnectionFactoryAttribute.ConfigType getConfigType() {
        return configType;
    }
}
