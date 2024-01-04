/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.client.outbound.connection.transaction.preparehalt;

import jakarta.ejb.Remote;

@Remote
public interface TransactionalRemote {
    void enlistOnePersistentXAResource();
    void intermittentCommitFailure();
    void intermittentCommitFailureTwoPhase();
}
