/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.transaction;

import jakarta.ejb.Remote;

@Remote
public interface SlowTransactionRemote {
    void doWork();
}
