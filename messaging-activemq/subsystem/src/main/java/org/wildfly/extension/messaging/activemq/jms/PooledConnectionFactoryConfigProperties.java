/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
