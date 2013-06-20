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
package org.wildfly.clustering.web.infinispan.sso.coarse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.clustering.web.sso.WebApplication;
import org.wildfly.clustering.web.sso.AuthenticationType;
import org.wildfly.clustering.web.sso.Credentials;

public class CoarseSSOCacheEntry<L> implements Credentials {
    private final AtomicReference<L> localContext = new AtomicReference<>();
    private volatile AuthenticationType authType;
    private volatile String user;
    private volatile String password;
    private final Map<WebApplication, String> sessions = new ConcurrentHashMap<>();

    @Override
    public AuthenticationType getAuthenticationType() {
        return this.authType;
    }

    @Override
    public void setAuthenticationType(AuthenticationType type) {
        this.authType = authType;
    }

    @Override
    public String getUser() {
        return this.user;
    }

    @Override
    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    public Map<WebApplication, String> getSessions() {
        return this.sessions;
    }

    public AtomicReference<L> getLocalContext() {
        return this.localContext;
    }
}
