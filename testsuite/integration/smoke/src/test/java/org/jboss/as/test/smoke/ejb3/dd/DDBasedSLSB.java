/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.ejb3.dd;

/**
 * @author Jaikiran Pai
 */
public class DDBasedSLSB implements Echo {

    @Override
    public String echo(String msg) {
        return msg;
    }
}
