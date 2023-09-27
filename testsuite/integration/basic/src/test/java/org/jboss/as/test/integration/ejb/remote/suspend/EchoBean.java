/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.suspend;

import jakarta.ejb.Stateful;

/**
 * @author Stuart Douglas
 */
@Stateful
public class EchoBean implements Echo {
    @Override
    public String echo(String val) {
        return val;
    }
}
