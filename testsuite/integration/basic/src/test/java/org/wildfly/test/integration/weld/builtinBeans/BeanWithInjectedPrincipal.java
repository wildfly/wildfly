/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.weld.builtinBeans;

import java.security.Principal;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;

import java.io.Serializable;

@SessionScoped
public class BeanWithInjectedPrincipal implements Serializable {

    @Inject
    Principal principal;

    public String getPrincipalName() {
        return principal.getName();
    }
}
