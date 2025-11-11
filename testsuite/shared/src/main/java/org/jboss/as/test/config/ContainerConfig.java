/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Configuration utility enum for managing container images used in the WildFly testsuite.
 * <p>
 * This enum provides centralized access to container image configurations for various services
 * including OpenTelemetry, Elasticsearch, MailServer, Kafka, Artemis, and Keycloak.
 * <p>
 * Configuration properties can be provided through multiple sources (in order of precedence):
 * <ol>
 *   <li>System properties (e.g., -Dtestsuite.kafka-native.image=apache/kafka-native:3.9.0)</li>
 *   <li>Environment variables (e.g., AS_TS_KAFKA_NATIVE_IMAGE=apache/kafka-native:3.9.0)</li>
 *   <li>Properties file specified via testsuite.config.properties system property or environment variable</li>
 *   <li>Default values hardcoded in this enum</li>
 * </ol>
 */
public enum ContainerConfig {

    /** OpenTelemetry Collector container configuration. */
    OTEL_COLLECTOR("testsuite.opentelemetry-collector.image", "otel/opentelemetry-collector:0.138.0"),

    /** Elasticsearch container configuration. */
    ELASTICSEARCH("testsuite.elasticsearch.image", "docker.elastic.co/elasticsearch/elasticsearch:8.15.4"),

    /** Mail Server container configuration. */
    MAILSERVER("testsuite.mailserver.image", "apache/james:demo-3.8.2"),

    /** Kafka container configuration. */
    KAFKA("testsuite.kafka-native.image", "apache/kafka-native:3.8.0"),

    /** Artemis container configuration. */
    ARTEMIS_BROKER("testsuite.activemq-artemis-broker.image", "quay.io/arkmq-org/activemq-artemis-broker:artemis.2.42.0"),

    /** Keycloak container configuration. */
    KEYCLOAK("testsuite.keycloak.image", "quay.io/keycloak/keycloak:24.0.5");

    private static final Logger logger = Logger.getLogger(ContainerConfig.class.getName());

    /** The separator used to split container image name and version. */
    private static final String VERSION_SEPARATOR = ":";

    /** System property or environment variable name for specifying the properties file location. */
    protected static final String PROPERTIES_FILE_PROPERTY_NAME = "testsuite.config.properties";

    /** Cached properties loaded from file. */
    private static Properties properties;

    /** Configuration key for the container image. */
    private final String configKey;

    /** Default container image. */
    private final String defaultImage;

    /**
     * Constructor for ContainerConfig enum.
     *
     * @param configKey the configuration key for the container image
     * @param defaultImage the default container image reference
     */
    ContainerConfig(String configKey, String defaultImage) {
        this.configKey = configKey;
        this.defaultImage = defaultImage;
    }

    /**
     * Tells if the input {@link CharSequence} is null or empty
     * @param cs {@link CharSequence} to check
     * @return true if the input {@link CharSequence} is null or empty
     */
    private static boolean isNullOrEmpty(final CharSequence cs) {
        return cs == null || cs.isEmpty();
    }

    /**
     * Loads and caches properties from the configuration file.
     * <p>
     * The properties file location can be specified via:
     * <ul>
     *   <li>System property: -Dtestsuite.config.properties=/path/to/testsuite-config.properties</li>
     *   <li>Environment variable: TESTSUITE_CONFIG_PROPERTIES=/path/to/testsuite-config.properties</li>
     * </ul>
     * If no file is specified, an empty Properties object is returned.
     *
     * @return the loaded properties, or an empty Properties object if no file is configured
     * @throws RuntimeException if the properties file cannot be loaded
     */
    private static Properties getPropertiesFromFile() {
        if (properties == null) {
            properties = new Properties();
            // Properties file location can be passed as e.g. -Dtestsuite.config.properties=/some-path/testsuite-config.properties
            String configFileLocation = System.getProperty(PROPERTIES_FILE_PROPERTY_NAME) != null ?
                    System.getProperty(PROPERTIES_FILE_PROPERTY_NAME) :
                    System.getenv(PROPERTIES_FILE_PROPERTY_NAME.replace(".", "_").toUpperCase(Locale.ROOT));
            if (!isNullOrEmpty(configFileLocation)) {
                logger.fine(String.format("Loading config properties from file: %s", configFileLocation));
                try (FileReader reader = new FileReader(convertToFile(configFileLocation))) {
                    properties.load(reader);
                } catch (IOException e) {
                    throw new RuntimeException(String.format("Could not load config properties from %s", configFileLocation), e);
                }
            }
        }
        return properties;
    }

    /**
     * Converts a file location string to a File object.
     * <p>
     * The conversion follows this logic:
     * <ol>
     *   <li>Attempts to parse the location as a URI</li>
     *   <li>If successful and the URI has a scheme (e.g., "file://"), converts the URI to a File</li>
     *   <li>If successful but the URI has no scheme, treats the URI path as a file system path</li>
     *   <li>If URI parsing fails, treats the location as a regular file system path</li>
     * </ol>
     *
     * @param configFileLocation the file location as a string (URI or file path)
     * @return the File object representing the location
     */
    private static File convertToFile(String configFileLocation) {
        try {
            URI uri = new URI(configFileLocation);
            // If the URI has a scheme (like "file://"), convert it to a File
            if (uri.getScheme() != null) {
                return new File(uri);
            }
            // If no scheme, treat the URI path as a regular file path
            return new File(uri.getPath());
        } catch (URISyntaxException | IllegalArgumentException e) {
            // Not a valid URI, treat as a regular file path
            return new File(configFileLocation);
        }
    }

    /**
     * Retrieves a configuration property value from multiple sources.
     * <p>
     * Checks sources in the following order of precedence:
     * <ol>
     *   <li>System property (e.g., -Dkey=value)</li>
     *   <li>Environment variable (key converted to uppercase with dots replaced by underscores)</li>
     *   <li>Properties file (if configured)</li>
     *   <li>Default value (if provided)</li>
     * </ol>
     *
     * @param key the property key
     * @param defaultValue the default value to use if the property is not found
     * @return the property value, or the default value if not found
     */
    private static String getProperty(String key, String defaultValue) {
        String value = isNullOrEmpty(System.getProperty(key)) ?
                System.getenv(key.replace(".", "_").toUpperCase(Locale.ROOT))
                :
                System.getProperty(key);
        logger.fine(String.format("Loading config properties from system property: %s = %s", key, value));
        return isNullOrEmpty(value) ? getPropertiesFromFile().getProperty(key, defaultValue) : value;
    }

    /**
     * Returns the full container image reference (name:version).
     *
     * @return the container image reference
     */
    public String getImage() {
        // Special handling for Keycloak's legacy property
        if (this == KEYCLOAK) {
            return System.getProperty("testsuite.integration.oidc.rhsso.image", getProperty(configKey, defaultImage));
        }
        return getProperty(configKey, defaultImage);
    }

    /**
     * Returns the name portion of the container image (before the colon).
     *
     * @return the container image name
     */
    public String getImageName() {
        return getImage().split(VERSION_SEPARATOR)[0];
    }

    /**
     * Returns the version portion of the container image (after the colon).
     *
     * @return the container image version
     */
    public String getImageVersion() {
        return getImage().split(VERSION_SEPARATOR)[1];
    }
}
