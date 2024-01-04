/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support;

import jakarta.inject.Inject;

public class Charlie {

    @Inject
    Alpha alpha;

    public String getAlphaId() {
        return alpha.getId();
    }

}
