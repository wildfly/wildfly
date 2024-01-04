/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.ejbref;

import jakarta.ejb.SessionContext;

/**
 * Checks whether injection via ejb-jar.xml of a SessionContext works.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 */
//@Stateless
public class CtxInjectionTesterBean implements CtxInjectionTester {
    // injected from ejb-jar.xml
    private SessionContext ctx;

    public void checkInjection() throws FailedException {
        if (ctx == null) { throw new FailedException(); }
    }
}
