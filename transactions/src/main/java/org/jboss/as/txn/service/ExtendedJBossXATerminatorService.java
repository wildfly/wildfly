/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.ExtendedJBossXATerminator;

/**
 * The ExtendedJBossXATerminator service.
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public final class ExtendedJBossXATerminatorService implements Service<ExtendedJBossXATerminator> {

    private final ExtendedJBossXATerminator value;

    public ExtendedJBossXATerminatorService(ExtendedJBossXATerminator value) {
        this.value = value;
    }

    public void start(StartContext context) throws StartException {
    }

    public void stop(StopContext context) {
    }

    public ExtendedJBossXATerminator getValue() throws IllegalStateException {
        return TxnServices.notNull(value);
    }
}
