/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.deployment;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class Echo implements Ping {

    @Override
    public void pong() {
    }

}
