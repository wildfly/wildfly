/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.session.oob;

import java.util.Set;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionManager;

/**
 * Out-of-band session attributes implementation, for use outside the context of a request.
 * @author Paul Ferraro
 */
public class OOBSessionAttributes<L, B extends Batch> implements SessionAttributes {

    private final SessionManager<L, B> manager;
    private final String id;

    public OOBSessionAttributes(SessionManager<L, B> manager, String id) {
        this.manager = manager;
        this.id = id;
    }

    @Override
    public Set<String> getAttributeNames() {
        try (B batch = this.manager.getBatcher().createBatch()) {
            ImmutableSession session = this.manager.readSession(this.id);
            if (session == null) {
                throw new IllegalStateException();
            }
            return session.getAttributes().getAttributeNames();
        }
    }

    @Override
    public Object getAttribute(String name) {
        try (B batch = this.manager.getBatcher().createBatch()) {
            ImmutableSession session = this.manager.readSession(this.id);
            if (session == null) {
                throw new IllegalStateException();
            }
            return session.getAttributes().getAttribute(name);
        }
    }

    @Override
    public Object removeAttribute(String name) {
        try (B batch = this.manager.getBatcher().createBatch()) {
            try (Session<L> session = this.manager.findSession(this.id)) {
                if (session == null) {
                    throw new IllegalStateException();
                }
                return session.getAttributes().removeAttribute(name);
            }
        }
    }

    @Override
    public Object setAttribute(String name, Object value) {
        try (B batch = this.manager.getBatcher().createBatch()) {
            try (Session<L> session = this.manager.findSession(this.id)) {
                if (session == null) {
                    throw new IllegalStateException();
                }
                return session.getAttributes().setAttribute(name, value);
            }
        }
    }
}
