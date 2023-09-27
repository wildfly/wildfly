/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.ejb.client.reconnect;

import jakarta.ejb.Remote;

@Remote
public interface SimpleCrashBeanRemote {
    String echo(String message);
}
