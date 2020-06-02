package org.wildfly.test.integration.microprofile.health.ear;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class War1ConfigSource implements ConfigSource {

    private final HashMap<String, String> props;

    public War1ConfigSource() {
        props = new HashMap<>();
        props.put(HealthTestConstants.PROPERTY_NAME_WAR1, Boolean.TRUE.toString());
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
