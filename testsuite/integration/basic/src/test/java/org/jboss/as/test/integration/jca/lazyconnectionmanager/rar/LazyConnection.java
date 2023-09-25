/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.lazyconnectionmanager.rar;

/**
 * @author <a href="mailto:jesper.pedersen@ironjacamar.org">Jesper Pedersen</a>
 * @author <a href="mailto:msimka@redhat.com">Martin Simka</a>
 */
public interface LazyConnection {
    boolean isManagedConnectionSet();

    boolean closeManagedConnection();

    boolean associate();

    boolean isEnlisted();

    boolean enlist();

    void close();
}
