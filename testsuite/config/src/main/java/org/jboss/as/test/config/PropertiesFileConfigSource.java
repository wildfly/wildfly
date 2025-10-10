package org.jboss.as.test.config;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A MicroProfile Config {@link ConfigSource} implementation that loads configuration from a properties file.
 * <p>
 * The properties file location can be specified via:
 * <ul>
 *   <li>System property: {@code -Dtestsuite.config.properties=/path/to/testsuite-config.properties}</li>
 *   <li>Environment variable: {@code TESTSUITE_CONFIG_PROPERTIES=/path/to/testsuite-config.properties}</li>
 * </ul>
 */
public class PropertiesFileConfigSource implements ConfigSource {

    Logger logger = Logger.getLogger(PropertiesFileConfigSource.class.getName());

    protected static final String PROPERTIES_FILE_PROPERTY_NAME = "testsuite.config.properties";
    Properties properties;

    /**
     * Loads properties from the configured file location.
     * <p>
     * The file path is resolved from the system property or environment variable
     * named {@value PROPERTIES_FILE_PROPERTY_NAME}. If the file cannot be loaded,
     * a {@link RuntimeException} is thrown.
     *
     * @return the loaded properties
     * @throws RuntimeException if the properties file cannot be read
     */
    private Properties getPropertiesFromFile() {
        if (properties == null) {
            properties = new Properties();
            // Properties file location can be passed as e.g. -Dtestsuite.config.properties=/some-path/testsuite-config.properties
            String configFileLocation = System.getProperty(PROPERTIES_FILE_PROPERTY_NAME) != null ?
                    System.getProperty(PROPERTIES_FILE_PROPERTY_NAME) :
                    System.getenv(PROPERTIES_FILE_PROPERTY_NAME.replace(".", "_").toUpperCase());
            if (configFileLocation != null) {
                logger.info(String.format("Loading config properties from file: %s", configFileLocation));
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
     * Converts a {@link String} in a {@link File}
     *
     * @param configFileLocation:
     *            String representing the file location as either a URI or a path
     *
     * @return {@link File} object corresponding to the input parameter
     */
    private File convertToFile(String configFileLocation) {
        try {
            return new File(new URI(configFileLocation));
        } catch (URISyntaxException | IllegalArgumentException e) {
            return Paths.get(configFileLocation).toFile();
        }
    }

    /**
     * Returns all properties from the configuration file as a map.
     *
     * @return a map of all property names and values
     */
    @Override
    public Map<String, String> getProperties() {
        Map<String, String> props = new HashMap<>();
        getPropertiesFromFile().stringPropertyNames().forEach(prop -> props.put(prop, getPropertiesFromFile().getProperty(prop)));
        return props;
    }

    /**
     * Returns the names of all properties in the configuration file.
     *
     * @return a set of property names
     */
    @Override
    public Set<String> getPropertyNames() {
        return getPropertiesFromFile().keySet().stream().map(Object::toString).collect(Collectors.toSet());
    }

    /**
     * Returns the value for the specified property key.
     *
     * @param name the property name
     * @return the property value, or {@code null} if not found
     */
    @Override
    public String getValue(String name) {
        return getPropertiesFromFile().getProperty(name);
    }

    /**
     * Returns the name of this configuration source.
     *
     * @return the simple class name of this config source
     */
    @Override
    public String getName() {
        return PropertiesFileConfigSource.class.getSimpleName();
    }
}
