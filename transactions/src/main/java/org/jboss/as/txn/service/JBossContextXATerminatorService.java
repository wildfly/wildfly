/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.txn.integration.JBossContextXATerminator;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.JBossXATerminator;
import org.wildfly.transaction.client.LocalTransactionContext;

/**
 * The XATerminator service for wildfly transaction client XATerminator.
 *
 * @author <a href="mailto:ochaloup@redhat.com">Ondrej Chaloupka</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class JBossContextXATerminatorService implements Service {
    private final Consumer<JBossContextXATerminator> contextXATerminatorConsumer;
    private final Supplier<JBossXATerminator> jbossXATerminatorSupplier;
    private final Supplier<LocalTransactionContext> localTransactionContextSupplier;

    public JBossContextXATerminatorService(final Consumer<JBossContextXATerminator> contextXATerminatorConsumer,
                                    final Supplier<JBossXATerminator> jbossXATerminatorSupplier,
                                    final Supplier<LocalTransactionContext> localTransactionContextSupplier) {
        this.contextXATerminatorConsumer = contextXATerminatorConsumer;
        this.jbossXATerminatorSupplier= jbossXATerminatorSupplier;
        this.localTransactionContextSupplier = localTransactionContextSupplier;
    }

    public void start(final StartContext context) throws StartException {
        contextXATerminatorConsumer.accept(new JBossContextXATerminator(localTransactionContextSupplier.get(), jbossXATerminatorSupplier.get()));
    }

    public void stop(final StopContext context) {
        contextXATerminatorConsumer.accept(null);
    }
}
