/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.ejb3.dd;

import jakarta.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
public class DDOverrideSLSB implements Echo {

    @Override
    public String echo(String msg) {
        return msg;
    }
}
