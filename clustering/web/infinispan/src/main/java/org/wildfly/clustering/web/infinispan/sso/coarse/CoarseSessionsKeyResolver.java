/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.infinispan.sso.coarse;

import java.util.function.Function;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.infinispan.spi.persistence.KeyFormat;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.web.infinispan.SessionKeyExternalizer;
import org.wildfly.clustering.web.infinispan.SessionKeyFormat;

/**
 * Externalizer for {@link CoarseSessionsKey}.
 * @author Paul Ferraro
 */
public enum CoarseSessionsKeyResolver implements Function<String, CoarseSessionsKey> {
    INSTANCE;

    @Override
    public CoarseSessionsKey apply(String id) {
        return new CoarseSessionsKey(id);
    }

    @MetaInfServices(Externalizer.class)
    public static class CoarseSessionsKeyExternalizer extends SessionKeyExternalizer<CoarseSessionsKey> {
        public CoarseSessionsKeyExternalizer() {
            super(CoarseSessionsKey.class, INSTANCE);
        }
    }

    @MetaInfServices(KeyFormat.class)
    public static class CoarseSessionsKeyFormat extends SessionKeyFormat<CoarseSessionsKey> {
        public CoarseSessionsKeyFormat() {
            super(CoarseSessionsKey.class, INSTANCE);
        }
    }
}
