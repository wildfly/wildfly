/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.jndi;

/**
 * @author Jaikiran Pai
 */
public interface RemoteEcho {

    String echo(String msg);

    EchoMessage echo(EchoMessage message);
}
