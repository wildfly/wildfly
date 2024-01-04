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
@Stateless(name = "TestEnvEntry")
@Remote(TestEnvEntry.class)
public class TestEnvEntryBean extends TestEnvEntryBeanBase implements TestEnvEntry {
    @Resource(name = "maxExceptions")
    private int maxExceptions = 4;

    @Resource
    private int numExceptions = 3;

    @Resource
    SessionContext sessionCtx;

    private int minExceptions = 1;

    public int getMaxExceptions() {
        return this.maxExceptions;
    }

    public int getNumExceptions() {
        return this.numExceptions;
    }

    public int getMinExceptions() {
        return this.minExceptions;
    }

    public SessionContext getSessionContext() {
        return this.sessionCtx;
    }

}
