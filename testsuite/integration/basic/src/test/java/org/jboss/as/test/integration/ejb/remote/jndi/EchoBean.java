/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.jndi;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
@Remote(RemoteEcho.class)
public class EchoBean implements RemoteEcho {

    @Override
    public String echo(String msg) {
        return msg;
    }

    @Override
    public EchoMessage echo(final EchoMessage message) {
        final EchoMessage echo = new EchoMessage();
        if (message != null) {
            echo.setMessage(message.getMessage());
        }
        return echo;
    }
}
