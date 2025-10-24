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
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Configuration utility class for managing container images used in the WildFly testsuite.
 * <p>
 * This class provides centralized access to container image configurations for various services
 * including OpenTelemetry, Elasticsearch, MailServer, Kafka, Artemis, and Keycloak.
 * <p>
 * Configuration properties can be provided through multiple sources (in order of precedence):
 * <ol>
 *   <li>System properties (e.g., -Das-ts.kafka-native.image=apache/kafka-native:3.9.0)</li>
 *   <li>Environment variables (e.g., AS_TS_KAFKA_NATIVE_IMAGE=apache/kafka-native:3.9.0)</li>
 *   <li>Properties file specified via testsuite.config.properties system property or environment variable</li>
 *   <li>Default values hardcoded in this class</li>
 * </ol>
 */
public class ContainerConfig {
    private static final Logger logger = Logger.getLogger(ContainerConfig.class.getName());

    /** The separator used to split container image name and version. */
    private static final String VERSION_SEPARATOR = ":";

    /** System property or environment variable name for specifying the properties file location. */
    protected static final String PROPERTIES_FILE_PROPERTY_NAME = "testsuite.config.properties";

    /** Cached properties loaded from file. */
    private static Properties properties;

    /** Configuration key for OpenTelemetry Collector container image. */
    protected static final String OPENTELEMETRY_COLLECTOR_IMAGE_CONFIG_KEY = "as-ts.opentelemetry-collector.image";

    /** Default OpenTelemetry Collector container image. */
    protected static final String OPENTELEMETRY_COLLECTOR_DEFAULT_IMAGE = "otel/opentelemetry-collector:0.138.0";

    /** Configuration key for Elasticsearch container image. */
    protected static final String ELASTICSEARCH_IMAGE_CONFIG_KEY = "as-ts.elasticsearch.image";

    /** Default Elasticsearch container image. */
    protected static final String ELASTICSEARCH_DEFAULT_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:8.15.4";

    /** Configuration key for Mail Server container image. */
    protected static final String MAILSERVER_IMAGE_CONFIG_KEY = "as-ts.mailserver.image";

    /** Default Mail Server container image. */
    protected static final String MAILSERVER_DEFAULT_IMAGE = "apache/james:demo-3.8.2";

    /** Configuration key for Kafka container image. */
    protected static final String KAFKA_IMAGE_CONFIG_KEY = "as-ts.kafka-native.image";

    /** Default Kafka container image. */
    protected static final String KAFKA_DEFAULT_IMAGE = "apache/kafka-native:3.8.0";

    /** Configuration key for Artemis container image. */
    protected static final String ARTEMIS_IMAGE_CONFIG_KEY = "as-ts.activemq-artemis-broker.image";

    /** Default Artemis container image. */
    protected static final String ARTEMIS_DEFAULT_IMAGE = "quay.io/arkmq-org/activemq-artemis-broker:artemis.2.42.0";

    /** Configuration key for Keycloak container image. */
    protected static final String KEYCLOAK_IMAGE_CONFIG_KEY = "as-ts.keycloak.image";

    /** Default Keycloak container image. */
    protected static final String KEYCLOAK_DEFAULT_IMAGE = "quay.io/keycloak/keycloak:24.0.1";

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ContainerConfig() {
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
     * Attempts to parse the location as a URI first. If that fails, treats it as a file system path.
     *
     * @param configFileLocation the file location as a string (URI or file path)
     * @return the File object representing the location
     */
    private static File convertToFile(String configFileLocation) {
        try {
            return new File(new URI(configFileLocation));
        } catch (URISyntaxException | IllegalArgumentException e) {
            return Paths.get(configFileLocation).toFile();
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

    /*
        OpenTelemetry
     */

    /**
     * Returns the full OpenTelemetry Collector container image reference (name:version).
     *
     * @return the OpenTelemetry Collector image reference
     */
    public static String getOpenTelemetryCollectorImage() {
        return getProperty(OPENTELEMETRY_COLLECTOR_IMAGE_CONFIG_KEY, OPENTELEMETRY_COLLECTOR_DEFAULT_IMAGE);
    }

    /**
     * Returns the name portion of the OpenTelemetry Collector container image (before the colon).
     *
     * @return the OpenTelemetry Collector image name
     */
    public static String getOpenTelemetryCollectorImageName() {
        return getOpenTelemetryCollectorImage().split(VERSION_SEPARATOR)[0];
    }

    /**
     * Returns the version portion of the OpenTelemetry Collector container image (after the colon).
     *
     * @return the OpenTelemetry Collector image version
     */
    public static String getOpenTelemetryCollectorImageVersion() {
        return getOpenTelemetryCollectorImage().split(VERSION_SEPARATOR)[1];
    }

    /*
        Elasticsearch
     */

    /**
     * Returns the full Elasticsearch container image reference (name:version).
     *
     * @return the Elasticsearch image reference
     */
    public static String getElasticsearchImage() {
        return getProperty(ELASTICSEARCH_IMAGE_CONFIG_KEY, ELASTICSEARCH_DEFAULT_IMAGE);
    }

    /*
        MailServer
     */

    /**
     * Returns the full Mail Server container image reference (name:version).
     *
     * @return the Mail Server image reference
     */
    public static String getMailServerImage() {
        return getProperty(MAILSERVER_IMAGE_CONFIG_KEY, MAILSERVER_DEFAULT_IMAGE);
    }

    /*
        Kafka
     */

    /**
     * Returns the full Kafka container image reference (name:version).
     *
     * @return the Kafka image reference
     */
    public static String getKafkaImage() {
        return getProperty(KAFKA_IMAGE_CONFIG_KEY, KAFKA_DEFAULT_IMAGE);
    }

    /**
     * Returns the name portion of the Kafka container image (before the colon).
     *
     * @return the Kafka image name
     */
    public static String getKafkaImageName() {
        return getKafkaImage().split(VERSION_SEPARATOR)[0];
    }

    /**
     * Returns the version portion of the Kafka container image (after the colon).
     *
     * @return the Kafka image version
     */
    public static String getKafkaImageVersion() {
        return getKafkaImage().split(VERSION_SEPARATOR)[1];
    }

    /*
        Artemis
     */

    /**
     * Returns the full Artemis container image reference (name:version).
     *
     * @return the Artemis image reference
     */
    public static String getArtemisImage() {
        return getProperty(ARTEMIS_IMAGE_CONFIG_KEY, ARTEMIS_DEFAULT_IMAGE);
    }

    /*
        Keycloak
     */

    /**
     * Returns the full Keycloak container image reference (name:version).
     * <p>
     * Note: This method also checks the legacy property 'testsuite.integration.oidc.rhsso.image'
     * for backwards compatibility.
     *
     * @return the Keycloak image reference
     */
    public static String getKeycloakImage() {
        return System.getProperty("testsuite.integration.oidc.rhsso.image", getProperty(KEYCLOAK_IMAGE_CONFIG_KEY, KEYCLOAK_DEFAULT_IMAGE));
    }
}
