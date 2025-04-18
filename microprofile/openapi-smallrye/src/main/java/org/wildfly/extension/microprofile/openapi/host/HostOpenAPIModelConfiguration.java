/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.host;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Encapsulates the OpenAPI endpoint configuration for a host.
 * @author Paul Ferraro
 */
public class HostOpenAPIModelConfiguration implements OpenAPIModelConfiguration {

    private final Config config;
    private final String serverName;
    private final String hostName;

    public HostOpenAPIModelConfiguration(String serverName, String hostName) {
        this.serverName = serverName;
        this.hostName = hostName;
        this.config = ConfigProvider.getConfig(OpenAPIModelConfiguration.class.getClassLoader());
    }

    @Override
    public String getServerName() {
        return this.serverName;
    }

    @Override
    public String getHostName() {
        return this.hostName;
    }

    @Override
    public String getPath() {
        return this.getProperty("path", String.class).orElse(DEFAULT_PATH);
    }

    @Override
    public boolean isEnabled() {
        return this.config.getOptionalValue(ENABLED, Boolean.class).orElse(Boolean.TRUE) && this.getProperty("enabled", Boolean.class).orElse(Boolean.TRUE);
    }

    @Override
    public Config getMicroProfileConfig() {
        return this.config;
    }

    private <T> Optional<T> getProperty(String name, Class<T> propertyType) {
        return this.config.getOptionalValue(String.format("mp.openapi.extensions.%s.%s.%s", this.serverName, this.hostName, name), propertyType);
    }
}
