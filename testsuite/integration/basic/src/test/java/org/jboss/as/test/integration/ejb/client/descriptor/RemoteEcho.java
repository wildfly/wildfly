/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.client.descriptor;

/**
 * @author Jaikiran Pai
 */
public interface RemoteEcho {

    String echo(String moduleName, String msg);

    String twoSecondEcho(final String moduleName, final String msg);
}
