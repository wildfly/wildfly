/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.security.plugins;

import java.security.Principal;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

import org.infinispan.commons.equivalence.AnyEquivalence;
import org.infinispan.util.concurrent.BoundedConcurrentHashMap;
import org.infinispan.util.concurrent.BoundedConcurrentHashMap.Eviction;
import org.jboss.security.authentication.JBossCachedAuthenticationManager.DomainInfo;

/**
 * Factory that creates default {@code ConcurrentMap}s for authentication cache.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class DefaultAuthenticationCacheFactory {

    /**
     * Returns a default cache implementation
     *
     * @return cache implementation
     */
    public ConcurrentMap<Principal, DomainInfo> getCache() {
        ConcurrentMap<Principal, DomainInfo> map = new BoundedConcurrentHashMap<Principal, DomainInfo>(
                1000, 16, Eviction.LIRS, new AuthenticationCacheEvictionListener(), AnyEquivalence.<Principal>getInstance(), AnyEquivalence.<DomainInfo>getInstance()) {

            private static final long serialVersionUID = 1459490003748298538L;

            /** {@inheritDoc} */
            @Override
            public DomainInfo remove(Object key) {
                DomainInfo removed = super.remove(key);
                if (removed != null) {
                    removed.logout();
                }
                return removed;
            }

            /** {@inheritDoc} */
            @Override
            public void clear() {
                Collection<DomainInfo> values = values();
                for (DomainInfo domainInfo : values) {
                    domainInfo.logout();
                }
                super.clear();
            }
        };
        return map;
    }
}
