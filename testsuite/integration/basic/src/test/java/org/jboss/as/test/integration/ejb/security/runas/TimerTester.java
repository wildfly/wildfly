/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runas;

/**
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
public interface TimerTester {
    void startTimer(long pPeriod);
}
