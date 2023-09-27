/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.jndi;

import jakarta.ejb.Local;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * User: jpai
 */
@Stateless
@LocalBean
@Remote(RemoteEcho.class)
@Local(Echo.class)
public class EchoBean implements Echo {

    public String echo(String msg) {
        return msg;
    }
}
