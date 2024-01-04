/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.sso.elytron;

import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * @author Paul Ferraro
 */
public interface LocalSSOContext {
    SecurityIdentity getSecurityIdentity();
    void setSecurityIdentity(SecurityIdentity identity);
}
