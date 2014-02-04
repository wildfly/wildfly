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

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.clustering.infinispan.invoker.Mutator;
import org.jboss.as.clustering.infinispan.invoker.Remover;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionContext;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Generic session implementation - independent of cache mapping strategy.
 * @author Paul Ferraro
 */
public class InfinispanSession<L> extends InfinispanImmutableSession implements Session<L> {

    private final SessionMetaData metaData;
    private final SessionAttributes attributes;
    private final AtomicReference<L> localContext;
    private final LocalContextFactory<L> localContextFactory;
    private final Mutator mutator;
    private final Remover<String> remover;
    private final AtomicBoolean valid = new AtomicBoolean(true);

    public InfinispanSession(String id, SessionMetaData metaData, SessionAttributes attributes, AtomicReference<L> localContext, LocalContextFactory<L> localContextFactory, SessionContext context, Mutator mutator, Remover<String> remover) {
        super(id, metaData, attributes, context);
        this.metaData = metaData;
        this.attributes = attributes;
        this.localContext = localContext;
        this.localContextFactory = localContextFactory;
        this.mutator = mutator;
        this.remover = remover;
    }

    @Override
    public SessionAttributes getAttributes() {
        if (!this.valid.get()) {
            throw InfinispanWebLogger.ROOT_LOGGER.invalidSession(this.getId());
        }
        return this.attributes;
    }

    @Override
    public void invalidate() {
        if (!this.valid.compareAndSet(true, false)) {
            throw InfinispanWebLogger.ROOT_LOGGER.invalidSession(this.getId());
        }
        this.remover.remove(this.getId());
    }

    @Override
    public boolean isValid() {
        return this.valid.get();
    }

    @Override
    public SessionMetaData getMetaData() {
        if (!this.valid.get()) {
            throw InfinispanWebLogger.ROOT_LOGGER.invalidSession(this.getId());
        }
        return this.metaData;
    }

    @Override
    public void close() {
        if (this.valid.get()) {
            this.metaData.setLastAccessedTime(new Date());
            this.mutator.mutate();
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
