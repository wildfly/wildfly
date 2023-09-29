/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session.fine;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.wildfly.clustering.web.session.ImmutableSessionAttributes;

/**
 * Exposes session attributes for fine granularity sessions.
 * @author Paul Ferraro
 */
public class FineImmutableSessionAttributes implements ImmutableSessionAttributes {
    private final Map<String, UUID> names;
    private final Map<UUID, Object> attributes;

    public FineImmutableSessionAttributes(Map<String, UUID> names, Map<UUID, Object> attributes) {
        this.names = names;
        this.attributes = attributes;
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.names.keySet();
    }

    @Override
    public Object getAttribute(String name) {
        UUID attributeId = this.names.get(name);
        return (attributeId != null) ? this.attributes.get(attributeId) : null;
    }
}
