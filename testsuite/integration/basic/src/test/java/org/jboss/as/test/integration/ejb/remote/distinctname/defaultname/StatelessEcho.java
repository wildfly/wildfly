/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.distinctname.defaultname;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
@Remote(Echo.class)
public class StatelessEcho implements Echo {

    @Override
    public String echo(String msg) {
        return msg;
    }
}
