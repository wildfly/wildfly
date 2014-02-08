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

import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.SSOManager;

public class InfinispanSSOManager<V, I, D, L> implements SSOManager<I, D, L> {

    private final SSOFactory<V, I, D, L> factory;
    private final Batcher batcher;
    private final IdentifierFactory<String> identifierFactory;

    public InfinispanSSOManager(SSOFactory<V, I, D, L> factory, IdentifierFactory<String> identifierFactory, Batcher batcher) {
        this.factory = factory;
        this.batcher = batcher;
        this.identifierFactory = identifierFactory;
    }

    @Override
    public SSO<I, D, L> createSSO(String ssoId) {
        return this.factory.createSSO(ssoId, this.factory.createValue(ssoId));
    }

    @Override
    public SSO<I, D, L> findSSO(String ssoId) {
        V value = this.factory.findValue(ssoId);
        return (value != null) ? this.factory.createSSO(ssoId, value) : null;
    }

    @Override
    public Batcher getBatcher() {
        return this.batcher;
    }

    @Override
    public String createIdentifier() {
        return this.identifierFactory.createIdentifier();
    }

    @Override
    public void start() {
        this.identifierFactory.start();
    }

    @Override
    public void stop() {
        this.identifierFactory.stop();
    }
}
