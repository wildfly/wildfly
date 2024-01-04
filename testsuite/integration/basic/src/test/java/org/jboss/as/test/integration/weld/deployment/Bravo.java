/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.deployment;

import jakarta.enterprise.context.Dependent;

@Dependent
public class Bravo implements Ping {

    @Override
    public void pong() {
    }

}
