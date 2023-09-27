/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.environment;

import jakarta.interceptor.InvocationContext;

import org.jboss.logging.Logger;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class XMLInterceptorB {
    private static final Logger log = Logger.getLogger(XMLInterceptorB.class);

    MySession2RemoteB session2;

    public Object intercept(InvocationContext ctx) throws Exception {
        log.trace("Calling XMLInterceptorB...");
        session2.doitSession();
        log.trace("Calling XMLInterceptorB - after doit");
        return false;
    }

}
