/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.remote.ejb;

import jakarta.ejb.Singleton;

/**
 * @author John Bailey, Ondrej Chaloupka
 */
@Singleton
public class Bean implements Remote {
    public String echo(String value) {
        return "Echo: " + value;
    }
}
