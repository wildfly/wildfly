/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.client.outbound.connection;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
@Remote(RemoteEcho.class)
public class IndependentBean implements RemoteEcho {
    @Override
    public String echo(String msg) {
        return msg;
    }
}
