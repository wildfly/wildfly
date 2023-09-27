/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.deployment;

public class EchoNotABean implements Ping {

    @Override
    public void pong() {
    }

}
