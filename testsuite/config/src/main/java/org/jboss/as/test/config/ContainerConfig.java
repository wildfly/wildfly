package org.jboss.as.test.config;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Utility class for retrieving container image configurations used in the testsuite.
 * <p>
 * This class provides configurable container images with default values for various services
 * such as OpenTelemetry, Elasticsearch, MailServer, Kafka, Artemis, and Keycloak.
 * Configuration values can be overridden using MicroProfile Config properties.
 */
public class ContainerConfig {
    private static final String VERSION_SEPARATOR = ":";
    private static Config config;

    protected static final String OPENTELEMETRY_COLLECTOR_IMAGE_CONFIG_KEY = "as-ts.opentelemetry-collector.image";
    protected static final String OPENTELEMETRY_COLLECTOR_DEFAULT_IMAGE = "otel/opentelemetry-collector:0.115.1";

    protected static final String ELASTICSEARCH_IMAGE_CONFIG_KEY = "as-ts.elasticsearch.image";
    protected static final String ELASTICSEARCH_DEFAULT_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:8.15.4";

    protected static final String MAILSERVER_IMAGE_CONFIG_KEY = "as-ts.mailserver.image";
    protected static final String MAILSERVER_DEFAULT_IMAGE = "apache/james:demo-3.8.2";

    protected static final String KAFKA_IMAGE_CONFIG_KEY = "as-ts.kafka-native.image";
    protected static final String KAFKA_DEFAULT_IMAGE = "apache/kafka-native:3.8.0";

    protected static final String ARTEMIS_IMAGE_CONFIG_KEY = "as-ts.activemq-artemis-broker.image";
    protected static final String ARTEMIS_DEFAULT_IMAGE = "quay.io/arkmq-org/activemq-artemis-broker:artemis.2.42.0";

    protected static final String KEYCLOAK_IMAGE_CONFIG_KEY = "as-ts.keycloak.image";
    protected static final String KEYCLOAK_DEFAULT_IMAGE = "quay.io/keycloak/keycloak:24.0.1";

    private ContainerConfig() {
    }

    /**
     * Lazily retrieves the MicroProfile Config instance.
     *
     * @return the Config instance
     */
    private static Config getConfig() {
        if (config == null) {
            config = ConfigProvider.getConfig();
        }
        return config;
    }

    /*
        OpenTelemetry
     */

    /**
     * Gets the full OpenTelemetry collector image reference (name:version).
     * <p>
     * Can be configured via the {@value OPENTELEMETRY_COLLECTOR_IMAGE_CONFIG_KEY} property.
     * Defaults to {@value OPENTELEMETRY_COLLECTOR_DEFAULT_IMAGE}.
     *
     * @return the OpenTelemetry collector image reference
     */
    public static String getOpenTelemetryCollectorImage() {
        return getConfig().getOptionalValue(OPENTELEMETRY_COLLECTOR_IMAGE_CONFIG_KEY, String.class).orElse(OPENTELEMETRY_COLLECTOR_DEFAULT_IMAGE);
    }

    /**
     * Gets the OpenTelemetry collector image name (without version).
     *
     * @return the OpenTelemetry collector image name
     */
    public static String getOpenTelemetryCollectorImageName() {
        return getOpenTelemetryCollectorImage().split(VERSION_SEPARATOR)[0];
    }

    /**
     * Gets the OpenTelemetry collector image version.
     *
     * @return the OpenTelemetry collector image version
     */
    public static String getOpenTelemetryCollectorImageVersion() {
        return getOpenTelemetryCollectorImage().split(VERSION_SEPARATOR)[1];
    }

    /*
        Elasticsearch
     */

    /**
     * Gets the Elasticsearch image reference.
     * <p>
     * Can be configured via the {@value ELASTICSEARCH_IMAGE_CONFIG_KEY} property.
     * Defaults to {@value ELASTICSEARCH_DEFAULT_IMAGE}.
     *
     * @return the Elasticsearch image reference
     */
    public static String getElasticsearchImage() {
        return getConfig().getOptionalValue(ELASTICSEARCH_IMAGE_CONFIG_KEY, String.class).orElse(ELASTICSEARCH_DEFAULT_IMAGE);
    }

    /*
        MailServer
     */

    /**
     * Gets the mail server image reference.
     * <p>
     * Can be configured via the {@value MAILSERVER_IMAGE_CONFIG_KEY} property.
     * Defaults to {@value MAILSERVER_DEFAULT_IMAGE}.
     *
     * @return the mail server image reference
     */
    public static String getMailServerImage() {
        return getConfig().getOptionalValue(MAILSERVER_IMAGE_CONFIG_KEY, String.class).orElse(MAILSERVER_DEFAULT_IMAGE);
    }

    /*
        Kafka
     */

    /**
     * Gets the full Kafka image reference (name:version).
     * <p>
     * Can be configured via the {@value KAFKA_IMAGE_CONFIG_KEY} property.
     * Defaults to {@value KAFKA_DEFAULT_IMAGE}.
     *
     * @return the Kafka image reference
     */
    public static String getKafkaImage() {
        return getConfig().getOptionalValue(KAFKA_IMAGE_CONFIG_KEY, String.class).orElse(KAFKA_DEFAULT_IMAGE);
    }

    /**
     * Gets the Kafka image name (without version).
     *
     * @return the Kafka image name
     */
    public static String getKafkaImageName() {
        return getKafkaImage().split(VERSION_SEPARATOR)[0];
    }

    /**
     * Gets the Kafka image version.
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
     * Gets the Artemis image reference.
     * <p>
     * Can be configured via the {@value ARTEMIS_IMAGE_CONFIG_KEY} property.
     * Defaults to {@value ARTEMIS_DEFAULT_IMAGE}.
     *
     * @return the Artemis image reference
     */
    public static String getArtemisImage() {
        return getConfig().getOptionalValue(ARTEMIS_IMAGE_CONFIG_KEY, String.class).orElse(ARTEMIS_DEFAULT_IMAGE);
    }

    /*
        Keycloak
     */

    /**
     * Gets the Keycloak image reference.
     * <p>
     * Can be configured via the {@value KEYCLOAK_IMAGE_CONFIG_KEY} property or
     * the {@code testsuite.integration.oidc.rhsso.image} system property.
     * Defaults to {@value KEYCLOAK_DEFAULT_IMAGE}.
     *
     * @return the Keycloak image reference
     */
    public static String getKeycloakImage() {
        return System.getProperty("testsuite.integration.oidc.rhsso.image", getConfig().getOptionalValue(KEYCLOAK_IMAGE_CONFIG_KEY, String.class).orElse(KEYCLOAK_DEFAULT_IMAGE));
    }
}
