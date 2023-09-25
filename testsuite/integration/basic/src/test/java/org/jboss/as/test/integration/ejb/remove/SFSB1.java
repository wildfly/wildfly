/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remove;

import jakarta.annotation.PreDestroy;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
public class SFSB1 {

    public static volatile boolean preDestroyCalled = false;

    private boolean shouldDenyDestruction = false;

    @PreDestroy
    private void preDestroy() {
        preDestroyCalled = true;
        if (shouldDenyDestruction) { throw new RuntimeException("Denying bean destruction"); }
    }

    // always throws a TransactionRequiredException
    @Remove
    public void done() {

    }

    @Remove
    public void doneAndDenyDestruction() {
        shouldDenyDestruction = true;
    }
}
