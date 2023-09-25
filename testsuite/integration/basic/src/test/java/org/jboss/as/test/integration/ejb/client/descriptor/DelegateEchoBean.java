/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.client.descriptor;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
@Remote(RemoteEcho.class)
public class DelegateEchoBean implements RemoteEcho {

    @Override
    public String echo(String moduleName, String msg) {
        return msg;
    }

    @Override
    public String twoSecondEcho(String moduleName, String msg) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return msg;
    }
}
