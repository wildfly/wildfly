/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.infinispan.session.fine;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.wildfly.clustering.ee.infinispan.GroupedKey;

/**
 * Cache key for session attributes.
 * @author Paul Ferraro
 */
public class SessionAttributeKey extends GroupedKey<String> implements org.wildfly.clustering.web.cache.session.fine.SessionAttributeKey {

    private final UUID attributeId;

    public SessionAttributeKey(Map.Entry<String, UUID> entry) {
        this(entry.getKey(), entry.getValue());
    }

    public SessionAttributeKey(String sessionId, UUID attributeId) {
        super(sessionId);
        this.attributeId = attributeId;
    }

    @Override
    public UUID getAttributeId() {
        return this.attributeId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClass(), this.getId(), this.attributeId);
    }

    @Override
    public boolean equals(Object object) {
        return super.equals(object) && (object instanceof SessionAttributeKey) && this.attributeId.equals(((SessionAttributeKey) object).attributeId);
    }

    @Override
    public String toString() {
        return String.format("%s(%s[%s])", SessionAttributeKey.class.getSimpleName(), this.getId(), this.attributeId);
    }
}