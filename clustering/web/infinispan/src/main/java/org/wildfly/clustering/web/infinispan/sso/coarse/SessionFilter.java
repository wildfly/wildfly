/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import java.util.Map;
import java.util.function.Predicate;

/**
 * Selects SSO sessions entries containing the specified session.
 * @author Paul Ferraro
 * @param <D> the deployment type
 */
public class SessionFilter<D> implements Predicate<Map.Entry<CoarseSessionsKey, Map<D, String>>> {

    private final String sessionId;

    public SessionFilter(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    @Override
    public boolean test(Map.Entry<CoarseSessionsKey, Map<D, String>> entry) {
        return entry.getValue().values().contains(this.sessionId);
    }
}
