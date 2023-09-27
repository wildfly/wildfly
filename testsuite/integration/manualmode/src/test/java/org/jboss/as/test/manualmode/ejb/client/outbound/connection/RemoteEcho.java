/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.client.outbound.connection;

/**
 * @author Jaikiran Pai
 */
public interface RemoteEcho {
    String echo(String msg);
}
