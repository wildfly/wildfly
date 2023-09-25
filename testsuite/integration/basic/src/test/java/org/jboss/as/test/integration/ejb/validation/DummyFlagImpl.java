/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.validation;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

@Startup
@Singleton
public class DummyFlagImpl implements DummyFlag {
    private boolean executedServiceCallFlag;

    public void setExecutedServiceCallFlag(boolean flag) {
        executedServiceCallFlag = flag;
    }

    @Override
    public void markAsExecuted() {
        executedServiceCallFlag = true;
    }

    @Override
    public void clearExecution() {
        executedServiceCallFlag = false;
    }

    public boolean getExecutedServiceCallFlag() {
        return executedServiceCallFlag;
    }
}
