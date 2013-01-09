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
import java.util.Map;
import java.util.Map.Entry;

import org.infinispan.util.concurrent.BoundedConcurrentHashMap.EvictionListener;
import org.jboss.security.authentication.JBossCachedAuthenticationManager.DomainInfo;

/**
 * Listener to perform a JAAS logout when an entry is evicted from the cache.
 *
 * TODO: https://issues.jboss.org/browse/AS7-6232
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class AuthenticationCacheEvictionListener implements EvictionListener<Principal, DomainInfo> {

    /** {@inheritDoc} */
    @Override
    public void onEntryEviction(Map<Principal, DomainInfo> evicted) {
        for (Entry<Principal, DomainInfo> entry : evicted.entrySet()) {
            DomainInfo domainInfo = entry.getValue();
            domainInfo.logout();
        }
    }

    @Override
    public void onEntryChosenForEviction(DomainInfo arg0) {
        // Do nothing
    }

    @Override
    public void onEntryActivated(Object key) {
        // Do nothing
    }

    @Override
    public void onEntryRemoved(Object key) {
        // Do nothing
    }
}
