/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.wildfly.clustering.web.session.ImmutableSessionAttributes;

/**
 * An immutable "snapshot" of a session's attributes which can be accessed outside the scope of a transaction.
 * @author Paul Ferraro
 */
public class SimpleImmutableSessionAttributes implements ImmutableSessionAttributes {

    private final Map<String, Object> attributes;

    public SimpleImmutableSessionAttributes(ImmutableSessionAttributes attributes) {
        Map<String, Object> map = new HashMap<>();
        for (String name: attributes.getAttributeNames()) {
            map.put(name, attributes.getAttribute(name));
        }
        this.attributes = Collections.unmodifiableMap(map);
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.attributes.keySet();
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }
}
