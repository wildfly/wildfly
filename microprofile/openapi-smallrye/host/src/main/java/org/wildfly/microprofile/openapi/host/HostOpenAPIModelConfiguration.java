/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.openapi.host;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASConfig;
import org.wildfly.microprofile.openapi.OpenAPIModelConfiguration;

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
        return this.getHostProperty(PATH, String.class).orElse(OpenAPIModelConfiguration.super.getPath());
    }

    @Override
    public boolean isEnabled() {
        return this.getHostProperty(ENABLED, Boolean.class).orElse(OpenAPIModelConfiguration.super.isEnabled());
    }

    @Override
    public Config getMicroProfileConfig() {
        return this.config;
    }

    private <T> Optional<T> getHostProperty(String name, Class<T> propertyType) {
        return getProperty(this.config, this.serverName, this.hostName, name, propertyType);
    }

    static <T> Optional<T> getProperty(Config config, String serverName, String hostName, String name, Class<T> propertyType) {
        return config.getOptionalValue(String.format("%sserver.%s.host.%s.%s", OASConfig.EXTENSIONS_PREFIX, serverName, hostName, name), propertyType);
    }
}
