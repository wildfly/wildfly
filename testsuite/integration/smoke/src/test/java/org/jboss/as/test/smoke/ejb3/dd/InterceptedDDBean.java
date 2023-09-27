/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.ejb3.dd;


/**
 * User: jpai
 */
public class InterceptedDDBean implements Echo {

    @Override
    public String echo(String msg) {
        return msg;
    }
}
