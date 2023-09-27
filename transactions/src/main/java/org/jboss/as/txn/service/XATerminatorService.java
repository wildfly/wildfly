/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.JBossXATerminator;

/**
 * The XATerminator service.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public final class XATerminatorService implements Service<JBossXATerminator> {

    private final JBossXATerminator value;

    public XATerminatorService(final JBossXATerminator value) {
        this.value = value;
    }

    public void start(final StartContext context) throws StartException {
    }

    public void stop(final StopContext context) {
    }

    public JBossXATerminator getValue() throws IllegalStateException {
        return TxnServices.notNull(value);
    }
}
