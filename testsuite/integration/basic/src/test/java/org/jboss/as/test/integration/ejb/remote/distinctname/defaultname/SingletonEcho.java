/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.distinctname.defaultname;

import jakarta.ejb.Remote;
import jakarta.ejb.Singleton;

/**
 * @author Jaikiran Pai
 */
@Singleton
@Remote(Echo.class)
public class SingletonEcho implements Echo {
    @Override
    public String echo(String msg) {
        return msg;
    }
}
