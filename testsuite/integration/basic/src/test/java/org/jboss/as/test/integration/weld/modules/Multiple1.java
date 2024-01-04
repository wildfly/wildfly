/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.modules;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Stuart Douglas
 */
@ApplicationScoped
public class Multiple1 {

    public String getMessage() {
        return "hello";
    }
}
