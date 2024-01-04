/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.http;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

@Stateless
@Remote(EchoRemote.class)
public class EchoBean implements EchoRemote {
    @Override
    public String echo(String arg) {
        return arg;
    }
}
