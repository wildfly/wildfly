/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.securityapi;

import java.security.Principal;
import java.time.LocalDateTime;

import jakarta.security.enterprise.CallerPrincipal;

/**
 * A simple {@link jakarta.security.enterprise.CallerPrincipal} with a custom field and method.
 *
 * @author <a href="mailto:jrodri@redhat.com">Jessica Rodriguez</a>
 */
public class TestCustomPrincipal extends CallerPrincipal {

    private static final long serialVersionUID = -35690086418605259L;
    private final LocalDateTime currentLoginTime;
    private final Principal wrappedPrincipal;

    public TestCustomPrincipal(Principal principal) {
        super(principal.getName());
        this.wrappedPrincipal = principal;
        this.currentLoginTime = LocalDateTime.now();
    }

    public LocalDateTime getCurrentLoginTime() {
        return this.currentLoginTime;
    }

    @Override
    public String getName() {
        return wrappedPrincipal.getName();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TestCustomPrincipal) {
            return this.getName().equals(((TestCustomPrincipal) obj).getName());
        } else {
            return super.equals(obj);
        }
    }
}
