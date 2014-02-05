/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.undertow.sso;

import io.undertow.security.idm.Account;
import io.undertow.security.impl.SingleSignOn;
import io.undertow.security.impl.SingleSignOnManager;

import org.wildfly.clustering.web.Batch;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.sso.Authentication;
import org.wildfly.clustering.web.sso.AuthenticationType;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.SSOManager;

/**
 * Adapts an {@link SSOManager} to a {@link SingleSignOnManager}.
 * @author Paul Ferraro
 */
// TODO Leverage SingleSignOn.close() to consolidate batching
public class DistributableSingleSignOnManager implements SingleSignOnManager {

    private final SSOManager<Account, String, Void> manager;
    private final SessionManagerRegistry registry;

    public DistributableSingleSignOnManager(SSOManager<Account, String, Void> manager, SessionManagerRegistry registry) {
        this.manager = manager;
        this.registry = registry;
    }

    @Override
    public SingleSignOn createSingleSignOn(Account account, String mechanism) {
        String id = this.manager.createIdentifier();
        Batcher batcher = this.manager.getBatcher();
        try (Batch batch = batcher.startBatch()) {
            SSO<Account, String, Void> sso = this.manager.createSSO(id);
            Authentication<Account> authentication = sso.getAuthentication();
            authentication.setIdentity(account);
            authentication.setType(AuthenticationType.valueOf(mechanism));
            return new DistributableSingleSignOn(sso, this.registry, batcher);
        }
    }

    @Override
    public SingleSignOn findSingleSignOn(String id) {
        Batcher batcher = this.manager.getBatcher();
        try (Batch batch = batcher.startBatch()) {
            SSO<Account, String, Void> sso = this.manager.findSSO(id);
            return (sso != null) ? new DistributableSingleSignOn(sso, this.registry, batcher) : null;
        }
    }

    @Override
    public void removeSingleSignOn(String id) {
        Batcher batcher = this.manager.getBatcher();
        try (Batch batch = batcher.startBatch()) {
            SSO<Account, String, Void> sso = this.manager.findSSO(id);
            if (sso != null) {
                sso.invalidate();
            }
        }
    }
}
