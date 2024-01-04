/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.sso.elytron;

import java.util.function.Supplier;

import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * @author Paul Ferraro
 */
public enum LocalSSOContextFactory implements Supplier<LocalSSOContext> {
    INSTANCE;

    @Override
    public LocalSSOContext get() {
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
