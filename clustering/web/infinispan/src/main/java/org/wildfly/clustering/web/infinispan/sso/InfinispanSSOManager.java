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
package org.wildfly.clustering.web.infinispan.sso;

import org.infinispan.Cache;
import org.wildfly.clustering.web.Batch;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.SSOManager;

public class InfinispanSSOManager<V, L> implements SSOManager<L>, Batcher {

    private final SSOFactory<V, L> factory;
    final Cache<String, V> cache;

    public InfinispanSSOManager(SSOFactory<V, L> factory, Cache<String, V> cache) {
        this.factory = factory;
        this.cache = cache;
    }

    @Override
    public SSO<L> createSSO(String ssoId) {
        return this.factory.createSSO(ssoId, this.factory.createValue(ssoId));
    }

    @Override
    public SSO<L> findSSO(String ssoId) {
        V value = this.factory.findValue(ssoId);
        return (value != null) ? this.factory.createSSO(ssoId, value) : null;
    }

    @Override
    public Batch startBatch() {
        final boolean started = this.cache.startBatch();
        return new Batch() {
            @Override
            public void close() {
                this.end(true);
            }

            @Override
            public void discard() {
                this.end(false);
            }

            private void end(boolean success) {
                if (started) {
                    InfinispanSSOManager.this.cache.endBatch(success);
                }
            }
        };
    }

    @Override
    public Batcher getBatcher() {
        return this;
    }
}
