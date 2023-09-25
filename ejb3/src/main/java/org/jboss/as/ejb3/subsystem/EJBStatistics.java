/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class EJBStatistics {

    private static final EJBStatistics INSTANCE = new EJBStatistics();
    private volatile boolean enabled;

    private EJBStatistics() {}

    public boolean isEnabled() {
        return enabled;
    }

    void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public static EJBStatistics getInstance() {
        return INSTANCE;
    }

}
