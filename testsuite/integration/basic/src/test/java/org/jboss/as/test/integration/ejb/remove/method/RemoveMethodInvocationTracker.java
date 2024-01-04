/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remove.method;

/**
 * @author Jaikiran Pai
 */
public interface RemoveMethodInvocationTracker {

    boolean wasEjbRemoveCallbackInvoked();

    void ejbRemoveCallbackInvoked();
}
