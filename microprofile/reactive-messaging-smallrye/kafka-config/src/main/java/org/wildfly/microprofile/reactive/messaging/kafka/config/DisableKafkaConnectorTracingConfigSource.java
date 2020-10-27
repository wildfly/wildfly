/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.microprofile.reactive.messaging.kafka.config;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DisableKafkaConnectorTracingConfigSource implements ConfigSource {

    private Map<String, String> PROPERTIES = Collections.singletonMap("mp.messaging.connector.smallrye-kafka.tracing-enabled", "false");

    @Override
    public Map<String, String> getProperties() {
        return PROPERTIES;
    }

    @Override
    public String getValue(String propertyName) {
        return PROPERTIES.get(propertyName);
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrdinal() {
        // Make the ordinal high so it cannot be overridden
        return Integer.MAX_VALUE;
    }

    @Override
    public Set<String> getPropertyNames() {
        return PROPERTIES.keySet();
    }
}
