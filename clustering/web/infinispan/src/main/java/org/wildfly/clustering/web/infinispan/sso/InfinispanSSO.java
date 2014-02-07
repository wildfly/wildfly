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
 **/
package org.wildfly.clustering.web.infinispan.sso;

import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.clustering.infinispan.invoker.Remover;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.sso.Authentication;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.Sessions;

public class InfinispanSSO<I, D, L> implements SSO<I, D, L> {
    private final String id;
    private final Authentication<I> authentication;
    private final Sessions<D> sessions;
    private final AtomicReference<L> localContext;
    private final LocalContextFactory<L> localContextFactory;
    private final Remover<String> remover;

    public InfinispanSSO(String id, Authentication<I> authentication, Sessions<D> sessions, AtomicReference<L> localContext, LocalContextFactory<L> localContextFactory, Remover<String> remover) {
        this.id = id;
        this.authentication = authentication;
        this.sessions = sessions;
        this.localContext = localContext;
        this.localContextFactory = localContextFactory;
        this.remover = remover;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public Authentication<I> getAuthentication() {
        return this.authentication;
    }

    @Override
    public Sessions<D> getSessions() {
        return this.sessions;
    }

    @Override
    public void invalidate() {
        this.remover.remove(this.id);
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
