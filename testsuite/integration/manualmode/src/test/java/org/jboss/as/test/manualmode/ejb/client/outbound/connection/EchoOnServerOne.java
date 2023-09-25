/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.client.outbound.connection;

import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
@Remote(RemoteEcho.class)
public class EchoOnServerOne implements RemoteEcho {
    @EJB (lookup = "ejb:/server-two-module//EchoOnServerTwo!org.jboss.as.test.manualmode.ejb.client.outbound.connection.RemoteEcho")
    private RemoteEcho echoOnServerTwo;

    @Override
    public String echo(String msg) {
        return this.echoOnServerTwo.echo(msg);
    }
}
