/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.deployment.enterprise;

import jakarta.ejb.Stateful;

@Stateful
public class Alpha implements AlphaLocal {

    @Override
    public void pong() {
    }

}
