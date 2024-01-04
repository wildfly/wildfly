/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

public class BravoLookup {

    @Inject
    private Instance<Comparable<Integer>> instance;

    public Instance<Comparable<Integer>> getInstance() {
        return instance;
    }
}
