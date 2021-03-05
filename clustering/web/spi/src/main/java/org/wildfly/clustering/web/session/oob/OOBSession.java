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

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Out-of-band session implementation, for use outside the context of a request.
 * @author Paul Ferraro
 */
public class OOBSession<L, B extends Batch> implements Session<L> {

    private final SessionManager<L, B> manager;
    private final String id;
    private final L localContext;
    private final SessionMetaData metaData;
    private final SessionAttributes attributes;

    public OOBSession(SessionManager<L, B> manager, String id, L localContext) {
        this.manager = manager;
        this.id = id;
        this.localContext = localContext;
        this.metaData = new OOBSessionMetaData<>(manager, id);
        this.attributes = new OOBSessionAttributes<>(manager, id);
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public boolean isValid() {
        try (B batch = this.manager.getBatcher().createBatch()) {
            return this.manager.readSession(this.id) != null;
        }
    }

    @Override
    public SessionMetaData getMetaData() {
        return this.metaData;
    }

    @Override
    public void invalidate() {
        try (B batch = this.manager.getBatcher().createBatch()) {
            try (Session<L> session = this.manager.findSession(this.id)) {
                if (session == null) {
                    throw new IllegalStateException();
                }
                session.invalidate();
            }
        }
    }

    @Override
    public SessionAttributes getAttributes() {
        return this.attributes;
    }

    @Override
    public L getLocalContext() {
        return this.localContext;
    }

    @Override
    public void close() {
        // OOB session have no concept of lifecycle
    }
}
