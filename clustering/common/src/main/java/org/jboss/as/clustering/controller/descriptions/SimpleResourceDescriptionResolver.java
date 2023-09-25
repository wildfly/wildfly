/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.descriptions;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;

/**
 * Simple {@link org.jboss.as.controller.descriptions.ResourceDescriptionResolver} implementation
 * that uses a static name/description mapping.
 * @author Paul Ferraro
 */
public class SimpleResourceDescriptionResolver extends StandardResourceDescriptionResolver {
    final Map<String, String> descriptions = new HashMap<>();

    public SimpleResourceDescriptionResolver(String name, String description) {
        super(name, null, SimpleResourceDescriptionResolver.class.getClassLoader());
        this.descriptions.put(name, description);
    }

    @Override
    public ResourceBundle getResourceBundle(Locale locale) {
        return new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                return SimpleResourceDescriptionResolver.this.descriptions.get(key);
            }

            @Override
            protected Set<String> handleKeySet() {
                return SimpleResourceDescriptionResolver.this.descriptions.keySet();
            }

            @Override
            public Enumeration<String> getKeys() {
                return Collections.enumeration(this.handleKeySet());
            }
        };
    }

    public void addDescription(String key, String description) {
        this.descriptions.put(String.join(".", this.getKeyPrefix(), key), description);
    }
}
