/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.weld.services.bootstrap;

import jakarta.transaction.Synchronization;

/**
 *
 *  Stores NamespaceContextSelector during synchronization, and pushes it on top of the selector stack each time synchronization
 *  callback method is executed. This enables synchronization callbacks served by corba threads to work correctly.
 *
 *  @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

import org.jboss.as.naming.context.NamespaceContextSelector;

public class JTSSynchronizationWrapper implements Synchronization {

    private final Synchronization synchronization;
    private final NamespaceContextSelector selector;

    public JTSSynchronizationWrapper(final Synchronization synchronization) {
        this.synchronization = synchronization;
        selector = NamespaceContextSelector.getCurrentSelector();
    }

    @Override
    public void beforeCompletion() {
        try {
            NamespaceContextSelector.pushCurrentSelector(selector);
            synchronization.beforeCompletion();
        } finally {
            NamespaceContextSelector.popCurrentSelector();
        }
    }

    @Override
    public void afterCompletion(final int status) {
        try {
            NamespaceContextSelector.pushCurrentSelector(selector);
            synchronization.afterCompletion(status);

        } finally {
            NamespaceContextSelector.popCurrentSelector();
        }
    }

}
