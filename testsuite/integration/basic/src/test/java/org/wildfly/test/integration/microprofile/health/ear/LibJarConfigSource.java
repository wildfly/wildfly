package org.wildfly.test.integration.microprofile.health.ear;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class LibJarConfigSource implements ConfigSource {

    private final HashMap<String, String> props;

    public LibJarConfigSource() {
        props = new HashMap<>();
        props.put(HealthTestConstants.PROPERTY_NAME_LIB_JAR, Boolean.TRUE.toString());
    }

    @Override
    public Map<String, String> getProperties() {
        return props;
    }

    @Override
    public String getValue(String propertyName) {
        return props.get(propertyName);
    }

    @Override
    public String getName() {
        return getClass().getName();
    }
}
