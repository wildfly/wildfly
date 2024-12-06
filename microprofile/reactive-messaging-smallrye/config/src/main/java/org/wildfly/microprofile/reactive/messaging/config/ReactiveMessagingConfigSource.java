package org.wildfly.microprofile.reactive.messaging.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReactiveMessagingConfigSource implements ConfigSource {

    private static final String NAME = ReactiveMessagingConfigSource.class.getName();

    private static final Map<String, String> PROPERTIES;
    static {
        Map<String, String> map = new HashMap<>();
        map.put("smallrye-messaging-strict-binding", "true");
        PROPERTIES = Collections.unmodifiableMap(map);
    }

    private final Map<String, String> properties;

    public ReactiveMessagingConfigSource() {
        properties = new ConcurrentHashMap<>();
        properties.putAll(PROPERTIES);
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrdinal() {
        // Make the ordinal high so it cannot be overridden
        return Integer.MAX_VALUE;
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }
}