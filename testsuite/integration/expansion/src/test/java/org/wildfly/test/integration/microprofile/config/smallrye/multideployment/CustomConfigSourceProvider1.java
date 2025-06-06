/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.multideployment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class CustomConfigSourceProvider1 implements ConfigSourceProvider {

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        return Collections.singleton(new ConfigSource() {
            private final Map<String, String> properties;

            {
                properties = new HashMap<>();
                properties.put("prop.local1.csp.unique", "csp1-value");
                properties.put("prop.global.system.overridden.by.csp", "csp1-wins-system");
                properties.put("prop.global.subsystem.overridden.by.csp", "csp1-wins-subsystem");
                properties.put("prop.local.precedence.test", "from-csp1");
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
                return CustomConfigSourceProvider1.class.getSimpleName() + "-Source";
            }

            @Override
            public int getOrdinal() {
                return 800;
            }
        });
    }
}