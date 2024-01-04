/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.ejb;

import jakarta.ejb.Stateless;

/**
 * A simple unsecured stateless bean with simple echo method
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2018 Red Hat, Inc.
 */
@Stateless
public class EjbUnsecuredBean {

    public String echo(String msg) {
        return msg;
    }

}
