/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session.coarse;

import java.util.Map;
import java.util.Set;

import org.wildfly.clustering.web.session.ImmutableSessionAttributes;

/**
 * Exposes session attributes for a coarse granularity session.
 * @author Paul Ferraro
 */
public class CoarseImmutableSessionAttributes implements ImmutableSessionAttributes {
    private final Map<String, Object> attributes;

    public CoarseImmutableSessionAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
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
