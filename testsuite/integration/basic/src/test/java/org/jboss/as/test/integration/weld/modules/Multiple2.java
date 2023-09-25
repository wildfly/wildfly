/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.modules;

import jakarta.inject.Inject;

/**
 * @author Stuart Douglas
 */
public class Multiple2 {

    @Inject
    private Multiple1 multiple1;

    public String getMessage() {
        return multiple1.getMessage();
    }
}
