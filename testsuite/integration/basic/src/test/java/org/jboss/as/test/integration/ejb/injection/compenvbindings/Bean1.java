/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.compenvbindings;

import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless(name="bean")
public class Bean1 {
    public String name() {
        return "Bean1";
    }
}
