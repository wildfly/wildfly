/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.persistencecontext;

import jakarta.ejb.Remote;

/**
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
@Remote
public interface StatefulRemote extends AutoCloseable {
    int doit();

    void find(int id);

    @Override
    void close();
}
