/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.JBossXATerminator;

import java.util.function.Consumer;

/**
 * The XATerminator service.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class XATerminatorService implements Service {
    private final Consumer<JBossXATerminator> terminatorConsumer;
    private final boolean jts;

    public XATerminatorService(final Consumer<JBossXATerminator> terminatorConsumer, final boolean jts) {
        this.terminatorConsumer = terminatorConsumer;
        this.jts = jts;
    }

    public void start(final StartContext context) throws StartException {
        final JBossXATerminator terminator = jts ? new com.arjuna.ats.internal.jbossatx.jts.jca.XATerminator() : new com.arjuna.ats.internal.jbossatx.jta.jca.XATerminator();
        terminatorConsumer.accept(terminator);
    }

    public void stop(final StopContext context) {
        terminatorConsumer.accept(null);
    }
}
