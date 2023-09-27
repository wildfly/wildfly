/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.base;

import org.jboss.as.test.xts.util.EventLog;


/**
 * Base interface which is inherited by other service interfaces.
 * This interface is used in test cases to check results of tests.
 */
public interface BaseServiceInterface {
    EventLog getEventLog();

    void clearEventLog();
}
