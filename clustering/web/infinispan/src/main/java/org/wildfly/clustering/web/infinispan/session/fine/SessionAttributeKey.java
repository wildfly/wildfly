/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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