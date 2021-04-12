package org.wildfly.test.integration.microprofile.health;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class HealthConfigSource implements ConfigSource {

    private final HashMap<String, String> props;

    public HealthConfigSource() {
        props = new HashMap<>();
        props.put("org.wildfly.test.integration.microprofile.health.MyLiveProbe.propertyConfiguredByTheDeployment", Boolean.TRUE.toString());
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
        return "ConfigSource local to the deployment";
    }

    @Override
    public Set<String> getPropertyNames() {
        return props.keySet();
    }
}
