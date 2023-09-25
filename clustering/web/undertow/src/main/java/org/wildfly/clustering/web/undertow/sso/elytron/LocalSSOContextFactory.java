/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.sso.elytron;

import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * @author Paul Ferraro
 */
public class LocalSSOContextFactory implements LocalContextFactory<LocalSSOContext> {

    @Override
    public LocalSSOContext createLocalContext() {
        return new LocalSSOContext() {
            private volatile SecurityIdentity identity;

            @Override
            public SecurityIdentity getSecurityIdentity() {
                return this.identity;
            }

            @Override
            public void setSecurityIdentity(SecurityIdentity identity) {
                this.identity = identity;
            }
        };
    }
}
