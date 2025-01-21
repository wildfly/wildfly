/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_class;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class CustomConfigSource implements ConfigSource{
    public static final String PROP_NAME = "my.prop.from.class";
    public static final String PROP_VALUE = "I'm from a custom config source!";
    public static final String PROP_NAME_OVERRIDEN_BY_SERVICE_LOADER = "my.prop.from.class.overriden.service.loader";
    public static final String PROP_VALUE_OVERRIDEN_BY_SERVICE_LOADER = "I'm from a custom config source! However I should be " +
            "overriden by property from ConfigSource provided by service loader.";

    final Map<String, String> props;

    public CustomConfigSource() {
        props = new HashMap<>();
        props.put(PROP_NAME, PROP_VALUE);
        props.put(PROP_NAME_OVERRIDEN_BY_SERVICE_LOADER, PROP_VALUE_OVERRIDEN_BY_SERVICE_LOADER);
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(props);
    }

    @Override
    public String getValue(String s) {
        return props.get(s);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public Set<String> getPropertyNames() {
        return props.keySet();
    }
}
