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
package org.wildfly.clustering.web.infinispan.session;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Generic session implementation - independent of cache mapping strategy.
 * @author Paul Ferraro
 */
public class InfinispanSession<L> extends InfinispanImmutableSession implements Session<L> {

    private final InvalidatableSessionMetaData metaData;
    private final SessionAttributes attributes;
    private final AtomicReference<L> localContext;
    private final LocalContextFactory<L> localContextFactory;
    private final Remover<String> remover;

    public InfinispanSession(String id, InvalidatableSessionMetaData metaData, SessionAttributes attributes, AtomicReference<L> localContext, LocalContextFactory<L> localContextFactory, Remover<String> remover) {
        super(id, metaData, attributes);
        this.metaData = metaData;
        this.attributes = attributes;
        this.localContext = localContext;
        this.localContextFactory = localContextFactory;
        this.remover = remover;
    }

    @Override
    public SessionAttributes getAttributes() {
        return this.attributes;
    }

    @Override
    public void invalidate() {
        if (this.metaData.invalidate()) {
            this.remover.remove(this.getId());
        }
    }

    @Override
    public boolean isValid() {
        return this.metaData.isValid();
    }

    @Override
    public SessionMetaData getMetaData() {
        return this.metaData;
    }

    @Override
    public void close() {
        if (this.metaData.isValid()) {
            this.attributes.close();
            this.metaData.setLastAccessedTime(Instant.now());
        }
    }

    @Override
    public L getLocalContext() {
        if (this.localContextFactory == null) return null;
        L localContext = this.localContext.get();
        if (localContext == null) {
            localContext = this.localContextFactory.createLocalContext();
            if (!this.localContext.compareAndSet(null, localContext)) {
                return this.localContext.get();
            }
        }
        return localContext;
    }
}
