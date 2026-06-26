/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.noremote;

/**
 * @author Richard Achmatowicz
 */
abstract class CommonRemoteEcho implements RemoteEcho {

    @Override
    public String echo(String message) {
        return message;
    }
}
