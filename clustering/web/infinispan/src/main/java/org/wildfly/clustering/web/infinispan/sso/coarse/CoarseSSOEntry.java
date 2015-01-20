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

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple container for CoarseAuthenticationEntry and sessions map.
 * @author Paul Ferraro
 */
public class CoarseSSOEntry<A, D, L> {

    private final A authentication;
    private final AtomicReference<L> localContext;
    private final Map<D, String> sessions;

    public CoarseSSOEntry(A authentication, AtomicReference<L> localContext, Map<D, String> sessions) {
        this.authentication = authentication;
        this.localContext = localContext;
        this.sessions = sessions;
    }

    public A getAuthentication() {
        return this.authentication;
    }

    public AtomicReference<L> getLocalContext() {
        return this.localContext;
    }

    public Map<D, String> getSessions() {
        return this.sessions;
    }
}
