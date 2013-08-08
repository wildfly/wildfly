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
package org.wildfly.clustering.web.undertow.session;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.wildfly.clustering.web.session.ImmutableSession;

import io.undertow.server.session.Session;

/**
 * Abstract Undertow adapter for a session.
 * @author Paul Ferraro
 */
public abstract class AbstractSessionAdapter<S extends ImmutableSession> implements Session {

    /**
     * Returns a reference to the delegate session.
     * @return a session reference
     */
    protected abstract S getSession();

    @Override
    public String getId() {
        return this.getSession().getId();
    }

    @Override
    public long getCreationTime() {
        return this.getSession().getMetaData().getCreationTime().getTime();
    }

    @Override
    public long getLastAccessedTime() {
        return this.getSession().getMetaData().getLastAccessedTime().getTime();
    }

    @Override
    public int getMaxInactiveInterval() {
        return (int) this.getSession().getMetaData().getMaxInactiveInterval(TimeUnit.SECONDS);
    }

    @Override
    public Object getAttribute(String name) {
        return this.getSession().getAttributes().getAttribute(name);
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.getSession().getAttributes().getAttributeNames();
    }
}
