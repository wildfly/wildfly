/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remove.method;

import jakarta.ejb.Remote;
import jakarta.ejb.Singleton;

/**
 * @author Jaikiran Pai
 */
@Singleton
@Remote(RemoveMethodInvocationTracker.class)
public class RemoveMethodInvocationTrackerBean implements RemoveMethodInvocationTracker {

    private boolean ejbRemoveCallbackInvoked;

    @Override
    public boolean wasEjbRemoveCallbackInvoked() {
        return this.ejbRemoveCallbackInvoked;
    }

    @Override
    public void ejbRemoveCallbackInvoked() {
        this.ejbRemoveCallbackInvoked = true;
    }

}
