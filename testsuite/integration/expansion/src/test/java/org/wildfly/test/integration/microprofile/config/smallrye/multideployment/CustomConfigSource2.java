/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.multideployment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class CustomConfigSource2 implements ConfigSource {

    private final Map<String, String> properties;

    public CustomConfigSource2() {
        properties = new HashMap<>();
        properties.put("prop.local2.cs.unique", "cs2-value");
        properties.put("prop.global.system.overridden.by.cs", "cs2-wins-system");
        properties.put("prop.global.subsystem.overridden.by.cs", "cs2-wins-subsystem");
        properties.put("prop.local.precedence.test", "from-cs2");
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrdinal() {
        return 750;
    }
}