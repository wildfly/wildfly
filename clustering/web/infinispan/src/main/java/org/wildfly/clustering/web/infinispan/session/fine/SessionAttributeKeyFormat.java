/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

import java.util.UUID;
import java.util.function.Function;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.infinispan.spi.persistence.DelimitedKeyFormat;
import org.wildfly.clustering.infinispan.spi.persistence.KeyFormat;

@MetaInfServices(KeyFormat.class)
public class SessionAttributeKeyFormat extends DelimitedKeyFormat<SessionAttributeKey> {
    public SessionAttributeKeyFormat() {
        super(SessionAttributeKey.class, "#", new SessionAttributeKeyParser(), new SessionAttributeKeyFormatter());
    }

    static class SessionAttributeKeyFormatter implements Function<SessionAttributeKey, String[]> {

        @Override
        public String[] apply(SessionAttributeKey key) {
            return new String[] { key.getId(), key.getAttributeId().toString() };
        }
    }

    static class SessionAttributeKeyParser implements Function<String[], SessionAttributeKey> {

        @Override
        public SessionAttributeKey apply(String[] parts) {
            return new SessionAttributeKey(parts[0], UUID.fromString(parts[1]));
        }
    }
}