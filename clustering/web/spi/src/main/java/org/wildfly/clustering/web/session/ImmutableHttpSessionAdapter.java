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
package org.wildfly.clustering.web.session;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

/**
 * Adapts an {@link ImmutableSession} to the {@link HttpSession} interface.
 * @author Paul Ferraro
 */
public class ImmutableHttpSessionAdapter implements HttpSession {

    private final ImmutableSession session;
    private final ServletContext context;

    public ImmutableHttpSessionAdapter(ImmutableSession session, ServletContext context) {
        this.session = session;
        this.context = context;
    }

    @Override
    public long getCreationTime() {
        return this.session.getMetaData().getCreationTime().toEpochMilli();
    }

    @Override
    public String getId() {
        return this.session.getId();
    }

    @Override
    public long getLastAccessedTime() {
        return this.session.getMetaData().getLastAccessedTime().toEpochMilli();
    }

    @Override
    public ServletContext getServletContext() {
        return this.context;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        // No-op
    }

    @Override
    public int getMaxInactiveInterval() {
        return (int) this.session.getMetaData().getMaxInactiveInterval().get(ChronoUnit.SECONDS);
    }

    @Override
    public Object getAttribute(String name) {
        return this.session.getAttributes().getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(this.session.getAttributes().getAttributeNames());
    }

    @Override
    public void setAttribute(String name, Object value) {
        // No-op
    }

    @Override
    public void removeAttribute(String name) {
        // No-op
    }

    @Override
    public void invalidate() {
        // No-op
    }

    @Override
    public boolean isNew() {
        return this.session.getMetaData().isNew();
    }

    @Override
    @Deprecated
    public String[] getValueNames() {
        return Collections.list(this.getAttributeNames()).toArray(new String[0]);
    }

    @Override
    @Deprecated
    public Object getValue(String name) {
        return this.getAttribute(name);
    }

    @Override
    @Deprecated
    public void putValue(String name, Object value) {
        this.setAttribute(name, value);
    }

    @Override
    @Deprecated
    public void removeValue(String name) {
        this.removeAttribute(name);
    }

    @Deprecated
    @Override
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        return new javax.servlet.http.HttpSessionContext() {
            @Override
            public Enumeration<String> getIds() {
                return Collections.enumeration(Collections.<String>emptyList());
            }

            @Override
            public HttpSession getSession(String sessionId) {
                return null;
            }
        };
    }
}
