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
 * @author <a href="mailto:mjurc@redhat.com">Michal Jurc</a> (c) 2018 Red Hat, Inc.
 */
public class CustomConfigSourceAServiceLoader implements ConfigSource {

    public static final String PROP_NAME_SAME_ORDINALITY_OVERRIDE = "my.prop.from.class.overriden.same.ordinality";
    public static final String PROP_VALUE_SAME_ORDINALITY_OVERRIDE = "I'm from a custom config source provided by service " +
            "loader overriding ConfigSource with same ordinality since my FQCN is ranked higher lexicographically.";


    final Map<String, String> props;

    public CustomConfigSourceAServiceLoader() {
        props = new HashMap<>();
        props.put(PROP_NAME_SAME_ORDINALITY_OVERRIDE, PROP_VALUE_SAME_ORDINALITY_OVERRIDE);
    }

    @Override
    public int getOrdinal() {
        return 101;
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
