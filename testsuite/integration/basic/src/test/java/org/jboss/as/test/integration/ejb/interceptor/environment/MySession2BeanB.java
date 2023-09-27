/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.environment;

import jakarta.ejb.Stateless;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
@Stateless
public class MySession2BeanB implements MySession2RemoteB {
    private static final Logger log = Logger.getLogger(MySession2BeanB.class);

    public boolean doit() {
        log.trace("Calling MySession2BeanB doit...");
        return true;
    }

    public boolean doitSession() {
        log.trace("Calling MySession2BeanB doitSession...");
        return true;
    }
}
