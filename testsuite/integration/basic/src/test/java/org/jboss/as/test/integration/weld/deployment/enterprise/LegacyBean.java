/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.deployment.enterprise;

import jakarta.ejb.Stateless;

@Stateless
public class LegacyBean implements Ping {

    @Override
    public void pong() {
    }

}
