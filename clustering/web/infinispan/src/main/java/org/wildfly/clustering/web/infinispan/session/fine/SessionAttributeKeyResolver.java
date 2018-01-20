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

import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.infinispan.spi.persistence.KeyFormat;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.web.infinispan.IndexedSessionKeyExternalizer;
import org.wildfly.clustering.web.infinispan.IndexedSessionKeyFormat;

/**
 * Resolver for a {@link SessionAttributeKey}.
 * @author Paul Ferraro
 */
public enum SessionAttributeKeyResolver implements ToIntFunction<SessionAttributeKey>, BiFunction<String, Integer, SessionAttributeKey> {
    INSTANCE;

    @Override
    public SessionAttributeKey apply(String sessionId, Integer attributeId) {
        return new SessionAttributeKey(sessionId, attributeId);
    }

    @Override
    public int applyAsInt(SessionAttributeKey key) {
        return key.getAttributeId();
    }

    @MetaInfServices(Externalizer.class)
    public static class SessionAttributeKeyExternalizer extends IndexedSessionKeyExternalizer<SessionAttributeKey> {
        public SessionAttributeKeyExternalizer() {
            super(SessionAttributeKey.class, INSTANCE, INSTANCE);
        }
    }

    @MetaInfServices(KeyFormat.class)
    public static class SessionAttributeKeyFormat extends IndexedSessionKeyFormat<SessionAttributeKey> {
        public SessionAttributeKeyFormat() {
            super(SessionAttributeKey.class, INSTANCE, INSTANCE);
        }
    }
}
