/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.jgroups.auth.BinaryAuthToken;
import org.jboss.as.controller.PathAddress;

import java.nio.charset.StandardCharsets;

/**
 * @author Paul Ferraro
 */
public class PlainAuthTokenServiceConfigurator extends AuthTokenServiceConfigurator<BinaryAuthToken> {

    public PlainAuthTokenServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public BinaryAuthToken apply(String sharedSecret) {
        return new BinaryAuthToken(sharedSecret.getBytes(StandardCharsets.UTF_8));
    }
}
