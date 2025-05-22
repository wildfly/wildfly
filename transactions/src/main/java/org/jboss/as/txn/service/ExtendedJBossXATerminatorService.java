/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.ExtendedJBossXATerminator;

import java.util.function.Consumer;

/**
 * The ExtendedJBossXATerminator service.
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ExtendedJBossXATerminatorService implements Service {
    private final Consumer<ExtendedJBossXATerminator> extendedTerminatorConsumer;
    private final boolean jts;

    public ExtendedJBossXATerminatorService(final Consumer<ExtendedJBossXATerminator> extendedTerminatorConsumer, final boolean jts) {
        this.extendedTerminatorConsumer = extendedTerminatorConsumer;
        this.jts = jts;
    }

    public void start(final StartContext context) throws StartException {
        final ExtendedJBossXATerminator extendedTerminator = jts ? new com.arjuna.ats.internal.jbossatx.jts.jca.XATerminator() : new com.arjuna.ats.internal.jbossatx.jta.jca.XATerminator();
        extendedTerminatorConsumer.accept(extendedTerminator);
    }

    public void stop(final StopContext context) {
        extendedTerminatorConsumer.accept(null);
    }
}
