/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.deployment.enterprise;

import jakarta.ejb.Stateful;

@Stateful
public class Foxtrot implements FoxtrotLocal {

    @Override
    public void pong() {
    }

}
