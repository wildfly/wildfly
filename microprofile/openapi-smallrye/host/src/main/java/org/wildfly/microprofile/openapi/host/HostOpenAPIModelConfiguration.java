/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.openapi.host;

import java.util.Optional;

import io.smallrye.openapi.api.SmallRyeOASConfig;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASConfig;
import org.wildfly.microprofile.openapi.OpenAPIModelConfiguration;

/**
 * Encapsulates the OpenAPI endpoint configuration for a host.
 * @author Paul Ferraro
 */
public class HostOpenAPIModelConfiguration implements OpenAPIModelConfiguration {
    private static final String DELIMITER = ".";
    private static final String SMALLRYE_PREFIX = OASConfig.EXTENSIONS_PREFIX + "smallrye" + DELIMITER;
    private static final String EXTERNAL_DOCUMENTATION = "externalDocs";
    private static final String DESCRIPTION = "description";
    private static final String URL = "url";

    public static final String EXTERNAL_DOCUMENTATION_DESCRIPTION = String.join(DELIMITER, EXTERNAL_DOCUMENTATION, DESCRIPTION);
    public static final String EXTERNAL_DOCUMENTATION_URL = String.join(DELIMITER, EXTERNAL_DOCUMENTATION, URL);
    public static final String INFO_CONTACT_EMAIL = SmallRyeOASConfig.INFO_CONTACT_EMAIL.substring(SMALLRYE_PREFIX.length());
    public static final String INFO_CONTACT_NAME = SmallRyeOASConfig.INFO_CONTACT_NAME.substring(SMALLRYE_PREFIX.length());
    public static final String INFO_CONTACT_URL = SmallRyeOASConfig.INFO_CONTACT_URL.substring(SMALLRYE_PREFIX.length());
    public static final String INFO_DESCRIPTION = SmallRyeOASConfig.INFO_DESCRIPTION.substring(SMALLRYE_PREFIX.length());
    public static final String INFO_LICENSE_IDENTIFIER = SmallRyeOASConfig.INFO_LICENSE_IDENTIFIER.substring(SMALLRYE_PREFIX.length());
    public static final String INFO_LICENSE_NAME = SmallRyeOASConfig.INFO_LICENSE_NAME.substring(SMALLRYE_PREFIX.length());
    public static final String INFO_LICENSE_URL = SmallRyeOASConfig.INFO_LICENSE_URL.substring(SMALLRYE_PREFIX.length());
    public static final String INFO_SUMMARY = SmallRyeOASConfig.INFO_SUMMARY.substring(SMALLRYE_PREFIX.length());
    public static final String INFO_TERMS_OF_SERVICE = SmallRyeOASConfig.INFO_TERMS.substring(SMALLRYE_PREFIX.length());
    public static final String INFO_TITLE = SmallRyeOASConfig.INFO_TITLE.substring(SMALLRYE_PREFIX.length());
    public static final String INFO_VERSION = SmallRyeOASConfig.INFO_VERSION.substring(SMALLRYE_PREFIX.length());
    public static final String JSON_SCHEMA_DIALECT = "jsonSchemaDialect";
    public static final String VERSION = "version";

    private static final String AUTO_GENERATE_SERVERS = "auto-generate-servers";
    private static final String COMPONENT_KEY_FORMAT = "component-key-format";
    private static final String DEFAULT_COMPONENT_KEY_FORMAT = "%1$s%2$s"; // Parameters: context path, component identifier

    private final Config config;
    private final String serverName;
    private final String hostName;

    public HostOpenAPIModelConfiguration(String serverName, String hostName) {
        this.serverName = serverName;
        this.hostName = hostName;
        this.config = ConfigProvider.getConfig(HostOpenAPIModelConfiguration.class.getClassLoader());
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
        return this.getPropertyValue(PATH, String.class).orElse(OpenAPIModelConfiguration.super.getPath());
    }

    @Override
    public boolean isEnabled() {
        return this.getPropertyValue(ENABLED, Boolean.class).orElse(OpenAPIModelConfiguration.super.isEnabled());
    }

    @Override
    public Config getMicroProfileConfig() {
        return this.config;
    }

    boolean isServerAutoGenerationEnabled() {
        return this.getPropertyValue(AUTO_GENERATE_SERVERS, Boolean.class).orElse(Boolean.FALSE);
    }

    String getComponentKeyFormat() {
        return this.getPropertyValue(COMPONENT_KEY_FORMAT, String.class).orElse(DEFAULT_COMPONENT_KEY_FORMAT);
    }

    <T> Optional<T> getPropertyValue(String propertyName, Class<T> propertyType) {
        Optional<T> result = this.config.getOptionalValue(String.format("%sserver.%s.host.%s.%s", OASConfig.EXTENSIONS_PREFIX, this.serverName, this.hostName, propertyName), propertyType);
        if (result.isEmpty()) {
            result = this.config.getOptionalValue(String.format("%sserver.%s.%s", OASConfig.EXTENSIONS_PREFIX, this.serverName, propertyName), propertyType);
        }
        if (result.isEmpty()) {
            result = this.config.getOptionalValue(OASConfig.EXTENSIONS_PREFIX + propertyName, propertyType);
        }
        return result;
    }
}
