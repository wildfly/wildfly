/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.enventry;

import jakarta.annotation.Resource;
import jakarta.ejb.Remote;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateless(name = "ExtendedTestEnvEntry")
@Remote(TestEnvEntry.class)
public class ExtendedTestEnvEntryBean extends TestEnvEntryBeanBase {
    @Resource(name = "maxExceptions")
    private int maxExceptions = 3;

    @Resource
    private int numExceptions = 2;

    @Resource
    SessionContext sessionCtx;

    private int minExceptions = 0;

    public int getMaxExceptions() {
        return maxExceptions;
    }

    public int getNumExceptions() {
        return numExceptions;
    }

    public int getMinExceptions() {
        return minExceptions;
    }

    public SessionContext getSessionContext() {
        return this.sessionCtx;
    }

}
