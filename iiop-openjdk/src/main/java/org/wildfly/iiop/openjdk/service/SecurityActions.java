/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk.service;

import static java.security.AccessController.doPrivileged;

import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.security.manager.action.CreateThreadAction;

/**
 * <p>
 * This class defines actions that must be executed in privileged blocks.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
class SecurityActions {

    /**
     * <p>
     * Creates a thread with the specified {@code Runnable} and name.
     * </p>
     *
     * @param runnable   the {@code Runnable} to be set in the new {@code Thread}.
     * @param threadName the name of the new {@code Thread}.
     * @return the construct {@code Thread} instance.
     */
    static Thread createThread(final Runnable runnable, final String threadName) {
        return ! WildFlySecurityManager.isChecking() ? new Thread(runnable, threadName) : doPrivileged(new CreateThreadAction(runnable, threadName));
    }
}
